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
use crate::alarm::Alarm;
use crate::beacon::Beacon;
use crate::cabinetstyle::CabinetStyle;
use crate::camera::Camera;
use crate::commconfig::CommConfig;
use crate::commlink::CommLink;
use crate::controller::Controller;
use crate::error::{Error, Result};
use crate::fetch::{fetch_delete, fetch_get, fetch_patch, fetch_post};
use crate::geoloc::GeoLoc;
use crate::lanemarking::LaneMarking;
use crate::modem::Modem;
use crate::permission::Permission;
use crate::rampmeter::RampMeter;
use crate::role::Role;
use crate::user::User;
use crate::util::{Dom, HtmlStr};
use crate::weathersensor::WeatherSensor;
use percent_encoding::{utf8_percent_encode, NON_ALPHANUMERIC};
use serde::de::DeserializeOwned;
use serde_json::map::Map;
use serde_json::Value;
use std::borrow::{Borrow, Cow};
use std::fmt;
use wasm_bindgen::JsValue;
use web_sys::Document;

/// CSS class for titles
const TITLE: &str = "title";

/// CSS class for names
pub const NAME: &str = "ob_name";

/// Compact "Create" card
const CREATE_COMPACT: &str = "<span class='create'>Create 🆕</span>";

/// Location button
pub const LOC_BUTTON: &str =
    "<button id='ob_loc' type='button'>🗺️ Location</button>";

/// Edit button
pub const EDIT_BUTTON: &str =
    "<button id='ob_edit' type='button'>📝 Edit</button>";

/// Resource types
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Resource {
    Alarm,
    Beacon,
    CabinetStyle,
    Camera,
    CommConfig,
    CommLink,
    Controller,
    GeoLoc,
    LaneMarking,
    Modem,
    Permission,
    RampMeter,
    Role,
    User,
    WeatherSensor,
    Unknown,
}

/// Card View
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum View {
    /// Compact Create view
    CreateCompact,
    /// Create view
    Create,
    /// Compact view
    Compact,
    /// Status view
    Status,
    /// Edit view
    Edit,
    /// Location view
    Location,
    /// Search view
    Search,
}

/// Search term
enum Search {
    /// Empty search (matches anything)
    Empty(),
    /// Normal search
    Normal(String),
    /// Exact (multi-word) search
    Exact(String),
}

/// Ancillary card view data
pub trait AncillaryData: Default {
    type Primary;

    /// Get ancillary data URI
    fn uri(&self, _view: View, _pri: &Self::Primary) -> Option<Cow<str>> {
        None
    }

    /// Set ancillary JSON data
    fn set_json(
        &mut self,
        _view: View,
        _pri: &Self::Primary,
        _json: JsValue,
    ) -> Result<()> {
        Ok(())
    }
}

/// A card view of a resource
pub trait Card: Default + fmt::Display + DeserializeOwned {
    type Ancillary: AncillaryData<Primary = Self>;

    /// Create from a JSON value
    fn new(json: &JsValue) -> Result<Self> {
        Ok(json.into_serde::<Self>()?)
    }

    /// Set the name
    fn with_name(self, name: &str) -> Self;

    /// Get next suggested name
    fn next_name(_obs: &[Self]) -> String {
        "".into()
    }

    /// Get geo location name
    fn geo_loc(&self) -> Option<&str> {
        None
    }

    /// Check if a search string matches
    fn is_match(&self, _search: &str, _anc: &Self::Ancillary) -> bool {
        false
    }

    /// Convert to Create HTML
    fn to_html_create(&self, _anc: &Self::Ancillary) -> String {
        format!(
            "<div class='row'>\
              <label for='create_name'>Name</label>\
              <input id='create_name' maxlength='24' size='24' value='{self}'/>\
            </div>"
        )
    }

    /// Convert to HTML view
    fn to_html(&self, view: View, _anc: &Self::Ancillary) -> String;

    /// Get changed fields from Edit form
    fn changed_fields(doc: &Document, json: &JsValue) -> Result<String>;
}

impl Resource {
    /// Lookup a resource by name
    pub fn from_name(res: &str) -> Self {
        match res {
            Alarm::RESOURCE_N => Self::Alarm,
            Beacon::RESOURCE_N => Self::Beacon,
            CabinetStyle::RESOURCE_N => Self::CabinetStyle,
            Camera::RESOURCE_N => Self::Camera,
            CommConfig::RESOURCE_N => Self::CommConfig,
            CommLink::RESOURCE_N => Self::CommLink,
            Controller::RESOURCE_N => Self::Controller,
            GeoLoc::RESOURCE_N => Self::GeoLoc,
            LaneMarking::RESOURCE_N => Self::LaneMarking,
            Modem::RESOURCE_N => Self::Modem,
            Permission::RESOURCE_N => Self::Permission,
            RampMeter::RESOURCE_N => Self::RampMeter,
            Role::RESOURCE_N => Self::Role,
            User::RESOURCE_N => Self::User,
            WeatherSensor::RESOURCE_N => Self::WeatherSensor,
            _ => Self::Unknown,
        }
    }

    /// Get resource name
    pub const fn rname(self) -> &'static str {
        match self {
            Self::Alarm => Alarm::RESOURCE_N,
            Self::Beacon => Beacon::RESOURCE_N,
            Self::CabinetStyle => CabinetStyle::RESOURCE_N,
            Self::Camera => Camera::RESOURCE_N,
            Self::CommConfig => CommConfig::RESOURCE_N,
            Self::CommLink => CommLink::RESOURCE_N,
            Self::Controller => Controller::RESOURCE_N,
            Self::GeoLoc => GeoLoc::RESOURCE_N,
            Self::LaneMarking => LaneMarking::RESOURCE_N,
            Self::Modem => Modem::RESOURCE_N,
            Self::Permission => Permission::RESOURCE_N,
            Self::RampMeter => RampMeter::RESOURCE_N,
            Self::Role => Role::RESOURCE_N,
            Self::User => User::RESOURCE_N,
            Self::WeatherSensor => WeatherSensor::RESOURCE_N,
            Self::Unknown => "",
        }
    }

    /// Lookup display name
    pub const fn dname(self) -> &'static str {
        match self {
            Self::Alarm => "📢 Alarm",
            Self::Beacon => "🔆 Beacon",
            Self::CabinetStyle => "🗄️ Cabinet Style",
            Self::Camera => "🎥 Camera",
            Self::CommConfig => "📡 Comm Config",
            Self::CommLink => "🔗 Comm Link",
            Self::Controller => "🎛️ Controller",
            Self::GeoLoc => "🗺️ Location",
            Self::LaneMarking => "⛙ Lane Marking",
            Self::Modem => "🖀 Modem",
            Self::Permission => "🗝️ Permission",
            Self::RampMeter => "🚦 Ramp Meter",
            Self::Role => "💪 Role",
            Self::User => "👤 User",
            Self::WeatherSensor => "🌦️ Weather Sensor",
            Self::Unknown => "❓ Unknown",
        }
    }

    /// Get the URI of an object
    fn uri_name(self, name: &str) -> String {
        let rname = self.rname();
        let name = utf8_percent_encode(name, NON_ALPHANUMERIC);
        format!("/iris/api/{rname}/{name}")
    }

    /// Delete a resource by name
    pub async fn delete(self, name: &str) -> Result<()> {
        let uri = self.uri_name(name);
        fetch_delete(&uri).await
    }

    /// Lookup resource symbol
    pub fn symbol(self) -> &'static str {
        self.dname().split_whitespace().next().unwrap()
    }

    /// Fetch card list for a resource type
    pub async fn fetch_list(self, search: &str) -> Result<String> {
        match self {
            Self::Alarm => fetch_list::<Alarm>(self, search).await,
            Self::Beacon => fetch_list::<Beacon>(self, search).await,
            Self::CabinetStyle => {
                fetch_list::<CabinetStyle>(self, search).await
            }
            Self::Camera => fetch_list::<Camera>(self, search).await,
            Self::CommConfig => fetch_list::<CommConfig>(self, search).await,
            Self::CommLink => fetch_list::<CommLink>(self, search).await,
            Self::Controller => fetch_list::<Controller>(self, search).await,
            Self::LaneMarking => fetch_list::<LaneMarking>(self, search).await,
            Self::Modem => fetch_list::<Modem>(self, search).await,
            Self::Permission => fetch_list::<Permission>(self, search).await,
            Self::RampMeter => fetch_list::<RampMeter>(self, search).await,
            Self::Role => fetch_list::<Role>(self, search).await,
            Self::User => fetch_list::<User>(self, search).await,
            Self::WeatherSensor => {
                fetch_list::<WeatherSensor>(self, search).await
            }
            _ => unreachable!(),
        }
    }

    /// Fetch a card for a given view
    pub async fn fetch_card(self, name: &str, view: View) -> Result<String> {
        match view {
            View::CreateCompact => Ok(CREATE_COMPACT.into()),
            View::Create => {
                let html = self.card_view(View::Create, name).await?;
                Ok(html_card_create(self.dname(), &html))
            }
            View::Compact => self.card_view(View::Compact, name).await,
            View::Location => match self.fetch_geo_loc(name).await? {
                Some(geo_loc) => card_location(&geo_loc).await,
                None => unreachable!(),
            },
            View::Status if self.has_status() => {
                let html = self.card_view(View::Status, name).await?;
                Ok(self.html_card_status(name, &html))
            }
            _ => {
                let html = self.card_view(View::Edit, name).await?;
                Ok(html_card_edit(self.dname(), name, &html))
            }
        }
    }

    /// Fetch a card view
    async fn card_view(self, view: View, name: &str) -> Result<String> {
        match self {
            Self::Alarm => card_view::<Alarm>(self, view, name).await,
            Self::Beacon => card_view::<Beacon>(self, view, name).await,
            Self::CabinetStyle => {
                card_view::<CabinetStyle>(self, view, name).await
            }
            Self::Camera => card_view::<Camera>(self, view, name).await,
            Self::CommConfig => card_view::<CommConfig>(self, view, name).await,
            Self::CommLink => card_view::<CommLink>(self, view, name).await,
            Self::Controller => card_view::<Controller>(self, view, name).await,
            Self::GeoLoc => card_view::<GeoLoc>(self, view, name).await,
            Self::LaneMarking => {
                card_view::<LaneMarking>(self, view, name).await
            }
            Self::Modem => card_view::<Modem>(self, view, name).await,
            Self::Permission => card_view::<Permission>(self, view, name).await,
            Self::RampMeter => card_view::<RampMeter>(self, view, name).await,
            Self::Role => card_view::<Role>(self, view, name).await,
            Self::User => card_view::<User>(self, view, name).await,
            Self::WeatherSensor => {
                card_view::<WeatherSensor>(self, view, name).await
            }
            _ => unreachable!(),
        }
    }

    /// Check if a resource has a Status view
    fn has_status(self) -> bool {
        matches!(
            self,
            Self::Alarm
                | Self::Beacon
                | Self::Camera
                | Self::CommLink
                | Self::Controller
                | Self::GeoLoc
                | Self::LaneMarking
                | Self::RampMeter
                | Self::WeatherSensor
        )
    }

    /// Save changed fields on card
    pub async fn save(self, name: &str) -> Result<()> {
        if let Some(window) = web_sys::window() {
            if let Some(doc) = window.document() {
                let uri = self.uri_name(name);
                let json = fetch_get(&uri).await?;
                let changed = self.changed_fields(&doc, &json)?;
                fetch_patch(&uri, &changed.into()).await?;
            }
        }
        Ok(())
    }

    /// Get changed fields from an edit view
    fn changed_fields(self, doc: &Document, json: &JsValue) -> Result<String> {
        match self {
            Self::Alarm => Alarm::changed_fields(doc, json),
            Self::Beacon => Beacon::changed_fields(doc, json),
            Self::CabinetStyle => CabinetStyle::changed_fields(doc, json),
            Self::Camera => Camera::changed_fields(doc, json),
            Self::CommConfig => CommConfig::changed_fields(doc, json),
            Self::CommLink => CommLink::changed_fields(doc, json),
            Self::Controller => Controller::changed_fields(doc, json),
            Self::GeoLoc => GeoLoc::changed_fields(doc, json),
            Self::LaneMarking => LaneMarking::changed_fields(doc, json),
            Self::Modem => Modem::changed_fields(doc, json),
            Self::Permission => Permission::changed_fields(doc, json),
            Self::RampMeter => RampMeter::changed_fields(doc, json),
            Self::Role => Role::changed_fields(doc, json),
            Self::User => User::changed_fields(doc, json),
            Self::WeatherSensor => WeatherSensor::changed_fields(doc, json),
            Self::Unknown => Ok("".into()),
        }
    }

    /// Create a new object
    pub async fn create_and_post(self) -> Result<()> {
        if let Some(window) = web_sys::window() {
            if let Some(doc) = window.document() {
                let value = match self {
                    Resource::Permission => Permission::create_value(&doc)?,
                    _ => self.create_value(&doc)?,
                };
                let json = value.into();
                fetch_post(&format!("/iris/api/{}", self.rname()), &json)
                    .await?;
            }
        }
        Ok(())
    }

    /// Create a name value
    fn create_value(self, doc: &Document) -> Result<String> {
        if let Some(name) = doc.input_parse::<String>("create_name") {
            if !name.is_empty() {
                let mut obj = Map::new();
                obj.insert("name".to_string(), Value::String(name));
                return Ok(Value::Object(obj).to_string());
            }
        }
        Err(Error::NameMissing())
    }

    /// Fetch primary JSON resource
    async fn fetch_primary<C: Card>(self, name: &str) -> Result<C> {
        let uri = self.uri_name(name);
        let json = fetch_get(&uri).await?;
        Ok(C::new(&json)?)
    }

    /// Fetch geo location name (if any)
    pub async fn fetch_geo_loc(self, name: &str) -> Result<Option<String>> {
        match self {
            Self::Beacon => self.geo_loc::<Beacon>(name).await,
            Self::Camera => self.geo_loc::<Camera>(name).await,
            Self::Controller => self.geo_loc::<Controller>(name).await,
            Self::GeoLoc => Ok(Some(name.into())),
            Self::LaneMarking => self.geo_loc::<LaneMarking>(name).await,
            Self::RampMeter => self.geo_loc::<RampMeter>(name).await,
            Self::WeatherSensor => self.geo_loc::<WeatherSensor>(name).await,
            _ => Ok(None),
        }
    }

    /// Fetch geo location name
    async fn geo_loc<C: Card>(self, name: &str) -> Result<Option<String>> {
        let pri = self.fetch_primary::<C>(name).await?;
        match pri.geo_loc() {
            Some(geo_loc) => Ok(Some(geo_loc.to_string())),
            None => Ok(None),
        }
    }

    /// Build a status card
    fn html_card_status(self, name: &str, status: &str) -> String {
        let dname = self.dname();
        let name = HtmlStr::new(name);
        format!(
            "<div class='row'>\
              <span class='{TITLE}'>{dname}</span>\
              <span class='{TITLE}'>Status</span>\
              <span class='{NAME}'>{name}</span>\
              <button id='ob_close' type='button'>X</button>\
            </div>\
            {status}"
        )
    }
}

/// Fetch JSON array and build card list
async fn fetch_list<C: Card>(res: Resource, search: &str) -> Result<String> {
    let rname = res.rname();
    let json = fetch_get(&format!("/iris/api/{rname}")).await?;
    let search = Search::new(search);
    let mut html = String::new();
    html.push_str("<ul class='cards'>");
    let obs = json.into_serde::<Vec<C>>()?;
    let next_name = C::next_name(&obs);
    // the "Create" card has id "{rname}_" and next available name
    html.push_str(&format!(
        "<li id='{rname}_' name='{next_name}' class='card'>\
            {CREATE_COMPACT}\
        </li>"
    ));
    // Use default value for ancillary data lookup
    let pri = C::default();
    let anc = fetch_ancillary(View::Search, &pri).await?;
    for pri in obs.iter().filter(|pri| search.is_match(*pri, &anc)) {
        html.push_str(&format!(
            "<li id='{rname}_{pri}' name='{pri}' class='card'>"
        ));
        html.push_str(&pri.to_html(View::Compact, &anc));
        html.push_str("</li>");
    }
    html.push_str("</ul>");
    Ok(html)
}

/// Fetch ancillary data
async fn fetch_ancillary<C: Card>(view: View, pri: &C) -> Result<C::Ancillary> {
    let mut anc = C::Ancillary::default();
    while let Some(uri) = anc.uri(view, pri) {
        let json = fetch_get(uri.borrow()).await?;
        anc.set_json(view, pri, json)?;
    }
    Ok(anc)
}

/// Fetch a card view
async fn card_view<C: Card>(
    res: Resource,
    view: View,
    name: &str,
) -> Result<String> {
    let pri = if view == View::Create {
        C::default().with_name(name)
    } else {
        res.fetch_primary::<C>(name).await?
    };
    let anc = fetch_ancillary(view, &pri).await?;
    Ok(pri.to_html(view, &anc))
}

/// Fetch a Location card
async fn card_location(name: &str) -> Result<String> {
    let html = Resource::GeoLoc.card_view(View::Edit, name).await?;
    Ok(html_card_edit(Resource::GeoLoc.dname(), name, &html))
}

impl Search {
    /// Create a new search term
    fn new(se: &str) -> Self {
        let se = se.to_lowercase();
        if se.is_empty() {
            Search::Empty()
        } else if se.starts_with('"') && se.ends_with('"') {
            Search::Exact(se.trim_matches('"').to_string())
        } else {
            Search::Normal(se)
        }
    }

    /// Test if a card matches the search
    fn is_match<C: Card>(&self, pri: &C, anc: &C::Ancillary) -> bool {
        match self {
            Search::Empty() => true,
            Search::Normal(se) => se.split(' ').all(|s| pri.is_match(s, anc)),
            Search::Exact(se) => pri.is_match(se, anc),
        }
    }
}

impl View {
    /// Is the view compact?
    pub fn is_compact(self) -> bool {
        matches!(self, View::Compact | View::CreateCompact)
    }

    /// Is the view a create view?
    pub fn is_create(self) -> bool {
        matches!(self, View::Create | View::CreateCompact)
    }

    /// Get compact view
    pub fn compact(self) -> Self {
        if self.is_create() {
            View::CreateCompact
        } else {
            View::Compact
        }
    }
}

/// Get attribute for disabled cards
pub fn disabled_attr(enabled: bool) -> &'static str {
    if enabled {
        ""
    } else {
        " class='disabled'"
    }
}

/// Build a create card
fn html_card_create(dname: &'static str, create: &str) -> String {
    format!(
        "<div class='row'>\
          <span class='{TITLE}'>{dname}</span>\
          <span class='{TITLE}'>Create</span>\
          <span class='{NAME}'>🆕</span>\
          <button id='ob_close' type='button'>X</button>\
        </div>\
        {create}
        <div class='row'>\
          <span></span>\
          <button id='ob_save' type='button'>🖍️ Save</button>\
        </div>"
    )
}

/// Build an edit card
fn html_card_edit(dname: &'static str, name: &str, edit: &str) -> String {
    let name = HtmlStr::new(name);
    format!(
        "<div class='row'>\
          <span class='{TITLE}'>{dname}</span>\
          <span class='{TITLE}'>Edit</span>\
          <span class='{NAME}'>{name}</span>\
          <button id='ob_close' type='button'>X</button>\
        </div>\
        {edit}\
        <div class='row'>\
          <span></span>\
          <button id='ob_delete' type='button'>🗑️ Delete</button>\
          <button id='ob_save' type='button'>🖍️ Save</button>\
        </div>"
    )
}
