// Copyright (C) 2022  Minnesota Department of Transportation
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
use crate::device::{Device, DeviceAnc};
use crate::error::Result;
use crate::resource::{disabled_attr, Card, NAME};
use crate::util::{ContainsLower, Dom, HtmlStr, OptVal};
use serde::{Deserialize, Serialize};
use serde_json::map::Map;
use serde_json::Value;
use std::fmt;
use wasm_bindgen::JsValue;
use web_sys::Document;

/// Alarm
#[derive(Debug, Default, Deserialize, Serialize)]
pub struct Alarm {
    pub name: String,
    pub description: String,
    pub controller: Option<String>,
    pub state: bool,
    // full attributes
    pub pin: Option<u32>,
    pub trigger_time: Option<String>,
}

type AlarmAnc = DeviceAnc<Alarm>;

impl Alarm {
    /// Get the alarm state to display
    fn state(&self, long: bool) -> &'static str {
        match (self.controller.is_some(), self.state, long) {
            (true, false, false) => "👍",
            (true, false, true) => "clear 👍",
            (true, true, false) => "😧",
            (true, true, true) => "triggered 😧",
            (false, _, true) => "unknown ❓",
            _ => "❓",
        }
    }
}

impl fmt::Display for Alarm {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}", HtmlStr::new(&self.name))
    }
}

impl Device for Alarm {
    /// Get controller
    fn controller(&self) -> Option<&str> {
        self.controller.as_deref()
    }
}

impl Card for Alarm {
    const TNAME: &'static str = "Alarm";
    const SYMBOL: &'static str = "📢";
    const ENAME: &'static str = "📢 Alarm";
    const UNAME: &'static str = "alarm";
    const HAS_STATUS: bool = true;

    type Ancillary = AlarmAnc;

    /// Set the name
    fn with_name(mut self, name: &str) -> Self {
        self.name = name.to_string();
        self
    }

    /// Check if a search string matches
    fn is_match(&self, search: &str, _anc: &AlarmAnc) -> bool {
        self.description.contains_lower(search)
            || self.name.contains_lower(search)
            || self.state(true).contains(search)
    }

    /// Convert to compact HTML
    fn to_html_compact(&self) -> String {
        let state = self.state(false);
        let description = HtmlStr::new(&self.description);
        let disabled = disabled_attr(self.controller.is_some());
        format!(
            "<span>{state}</span>\
            <span{disabled}>{description}</span>\
            <span class='{NAME}'>{self}</span>"
        )
    }

    /// Convert to status HTML
    fn to_html_status(&self, _anc: &AlarmAnc) -> String {
        let description = HtmlStr::new(&self.description);
        let state = self.state(true);
        let trigger_time = self.trigger_time.as_deref().unwrap_or("-");
        format!(
            "<div class='row'>\
              <span class='info'>{description}</span>\
              <span class='info'>{state}</span>\
            </div>\
            <div class='row'>\
              <span>Triggered</span>\
              <span class='info'>{trigger_time}</span>\
            </div>"
        )
    }

    /// Convert to edit HTML
    fn to_html_edit(&self, anc: &AlarmAnc) -> String {
        let ctrl_loc = anc.controller_loc_html();
        let description = HtmlStr::new(&self.description);
        let controller = HtmlStr::new(&self.controller);
        let pin = OptVal(self.pin);
        format!(
            "{ctrl_loc}\
            <div class='row'>\
              <label for='edit_desc'>Description</label>\
              <input id='edit_desc' maxlength='24' size='24' \
                     value='{description}'/>\
             </div>\
             <div class='row'>\
               <label for='edit_ctrl'>Controller</label>\
               <input id='edit_ctrl' maxlength='20' size='20' \
                      value='{controller}'/>\
             </div>\
             <div class='row'>\
               <label for='edit_pin'>Pin</label>\
               <input id='edit_pin' type='number' min='1' max='104' \
                      size='8' value='{pin}'/>\
             </div>"
        )
    }

    /// Get changed fields from Edit form
    fn changed_fields(doc: &Document, json: &JsValue) -> Result<String> {
        let val = Self::new(json)?;
        let mut obj = Map::new();
        if let Some(desc) = doc.input_parse::<String>("edit_desc") {
            if desc != val.description {
                obj.insert("description".to_string(), Value::String(desc));
            }
        }
        let ctrl = doc
            .input_parse::<String>("edit_ctrl")
            .filter(|c| !c.is_empty());
        if ctrl != val.controller {
            obj.insert("controller".to_string(), OptVal(ctrl).into());
        }
        let pin = doc.input_parse::<u32>("edit_pin");
        if pin != val.pin {
            obj.insert("pin".to_string(), OptVal(pin).into());
        }
        Ok(Value::Object(obj).to_string())
    }
}
