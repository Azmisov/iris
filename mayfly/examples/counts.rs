use mayfly::binned::CountData;
use mayfly::vehicle::{VehLog, VehicleFilter};
use std::fs::File;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let file = File::open("res/test.vlog")?;
    let vlog = VehLog::from_blocking_reader(file)?;
    print!("[");
    for (i, v) in vlog
        .binned_iter::<CountData>(30, VehicleFilter::default())
        .enumerate()
    {
        if i > 0 {
            print!(",");
        }
        print!("{}", v);
    }
    println!("]");

    Ok(())
}
