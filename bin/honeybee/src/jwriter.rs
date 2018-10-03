/*
 * Copyright (C) 2018  Minnesota Department of Transportation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
use failure::Error;
use fallible_iterator::FallibleIterator;
use postgres::{Connection,TlsMode};
use ssh2::Session;
use std::collections::HashSet;
use std::fs::File;
use std::io;
use std::net::TcpStream;
use std::path::PathBuf;
use std::sync::mpsc::{channel,Receiver,Sender};
use std::thread;
use std::time::{Duration,Instant};
use resource::{lookup_resource,Resource};

static OUTPUT_DIR: &str = "/var/www/html/iris/";

pub fn start(username: String, host: Option<String>) -> Result<(), Error> {
    // Format path for unix domain socket
    let uds = format!("postgres://{:}@%2Frun%2Fpostgresql/tms", username);
    let (tx, rx) = channel();
    let db = thread::spawn(move || {
        db_thread(uds, tx).unwrap();
    });
    if let Some(h) = host {
        scp_thread(h, username, rx);
    } else {
        null_thread(rx);
    }
    db.join().expect("db_thread panicked!");
    Ok(())
}

fn null_thread(rx: Receiver<Resource>) {
    for r in rx {
        println!("    {}: not copied (no destination host)", r.name());
    }
}

#[allow(dead_code)]
struct SshSession {
    tcp    : TcpStream, // must remain in scope as long as Session
    session: Session,
}

impl SshSession {
    fn new(username: &str, h: &str) -> Result<Self, Error> {
        let tcp = TcpStream::connect(h)?;
        let mut session = Session::new().unwrap();
        session.handshake(&tcp)?;
        // Try agent first, since we don't have a pass-phrase
        if let Err(_) = session.userauth_agent(username) {
            let mut key = PathBuf::new();
            key.push("/home");
            key.push(username);
            key.push(".ssh");
            key.push("id_rsa");
            // Since agent failed, try private key with no pass-phrase
            session.userauth_pubkey_file(username, None, &key, None)?;
        }
        Ok(SshSession { tcp, session })
    }
    fn do_session(&self, rx: &Receiver<Resource>,
        mut ns: &mut HashSet<Resource>)
    {
        loop {
            if ns.is_empty() {
                match rx.recv() {
                    Ok(r)  => { ns.insert(r); },
                    Err(_) => { return; },
                }
            }
            for r in rx.try_iter() {
                ns.insert(r);
            }
            if let Err(e) = self.copy_all(&mut ns) {
                println!("scp_file error: {}", e);
                thread::sleep(Duration::from_secs(10));
                return;
            }
        }
    }
    fn copy_all(&self, ns: &mut HashSet<Resource>) -> Result<(), Error> {
        for r in ns.iter() {
            let t = Instant::now();
            self.scp_file(&r)?;
            println!("    {}: copied in {:?}", r.name(), t.elapsed());
        }
        // All copied successfully
        ns.clear();
        Ok(())
    }
    fn scp_file(&self, r: &Resource) -> Result<(), Error> {
        let p = r.make_name(OUTPUT_DIR);
        let mut fi = File::open(&p)?;
        let m = fi.metadata()?;
        let mut fo = self.session.scp_send(p.as_path(), 0o644, m.len(), None)?;
        let c = io::copy(&mut fi, &mut fo)?;
        if c != m.len() {
            println!("    {}: length mismatch {} != {}", r.name(), c, m.len());
        }
        Ok(())
    }
}

fn scp_thread(host: String, username: String, rx: Receiver<Resource>) {
    let mut ns = HashSet::new();
    loop {
        match SshSession::new(&username, &host) {
            Ok(s)  => s.do_session(&rx, &mut ns),
            Err(e) => {
                println!("SshSession::new error: {}", e);
                thread::sleep(Duration::from_secs(10));
            },
        }
    }
}

fn db_thread(uds: String, tx: Sender<Resource>) -> Result<(), Error> {
    let conn = Connection::connect(uds, TlsMode::None)?;
    // The postgresql crate sets the session time zone to UTC.
    // We need to set it back to LOCAL time zone, so that row_to_json
    // can format properly (for incidents, etc).
    conn.execute("SET TIME ZONE 'US/Central'", &[])?;
    conn.execute("LISTEN tms", &[])?;
    // Initialize all the resources
    for r in ["camera_pub", "dms_pub", "dms_message", "incident", "sign_config",
              "parking_area", "parking_area_dynamic", "font"].iter()
    {
        fetch_resource_timed(&conn, &tx, r)?;
    }
    notify_loop(&conn, tx)
}

fn fetch_resource_timed(conn: &Connection, tx: &Sender<Resource>, n: &str)
    -> Result<(), Error>
{
    let t = Instant::now();
    if let Some(c) = fetch_resource_file(&conn, tx, &n)? {
        println!("{}: wrote {} rows in {:?}", &n, c, t.elapsed());
    } else {
        println!("{}: unknown resource", &n);
    }
    Ok(())
}

fn fetch_resource_file(conn: &Connection, tx: &Sender<Resource>, n: &str)
    -> Result<Option<u32>, Error>
{
    if let Some(r) = lookup_resource(n) {
        let c = r.fetch_file(&conn, OUTPUT_DIR)?;
        tx.send(r)?;
        Ok(Some(c))
    } else {
        Ok(None)
    }
}

fn notify_loop(conn: &Connection, tx: Sender<Resource>) -> Result<(), Error> {
    let nots = conn.notifications();
    let mut ns = HashSet::new();
    loop {
        for n in nots.timeout_iter(Duration::from_millis(300)).iterator() {
            let n = n?;
            ns.insert(n.payload);
        }
        for n in ns.drain() {
            fetch_resource_timed(&conn, &tx, &n)?;
        }
    }
}
