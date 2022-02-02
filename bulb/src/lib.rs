use percent_encoding::{utf8_percent_encode, NON_ALPHANUMERIC};
use std::cell::RefCell;
use std::fmt;
use wasm_bindgen::prelude::*;
use wasm_bindgen::JsCast;
use wasm_bindgen_futures::{spawn_local, JsFuture};
use web_sys::{
    console, Document, Element, Event, HtmlButtonElement, HtmlElement,
    HtmlInputElement, HtmlSelectElement, Request, Response, ScrollBehavior,
    ScrollIntoViewOptions, ScrollLogicalPosition, Window,
};

pub type Result<T> = std::result::Result<T, JsValue>;

mod alarm;
mod cabinetstyle;
mod card;
mod commconfig;
mod commlink;
mod controller;
mod modem;

use alarm::Alarm;
use cabinetstyle::CabinetStyle;
use card::{Card, CardType};
use commconfig::CommConfig;
use commlink::CommLink;
use controller::Controller;
use modem::Modem;

#[derive(Debug)]
struct OptVal<T>(Option<T>);

impl<T> fmt::Display for OptVal<T>
where
    T: fmt::Display,
{
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match &self.0 {
            Some(v) => write!(f, "{}", v),
            None => Ok(()),
        }
    }
}

#[derive(Debug)]
pub struct HtmlStr<S>(S);

impl<S> HtmlStr<S> {
    fn fmt_encode(s: &str, f: &mut fmt::Formatter) -> fmt::Result {
        for c in s.chars() {
            match c {
                '&' => write!(f, "&amp;")?,
                '<' => write!(f, "&lt;")?,
                '>' => write!(f, "&gt;")?,
                '"' => write!(f, "&quot;")?,
                '\'' => write!(f, "&#x27;")?,
                _ => write!(f, "{}", c)?,
            }
        }
        Ok(())
    }
}

impl fmt::Display for HtmlStr<&str> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        Self::fmt_encode(self.0, f)
    }
}

impl fmt::Display for HtmlStr<&String> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        Self::fmt_encode(self.0, f)
    }
}

impl fmt::Display for HtmlStr<Option<&String>> {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match &self.0 {
            Some(val) => Self::fmt_encode(val, f),
            None => Ok(()),
        }
    }
}

pub trait ElemCast {
    /// Get an element by ID and cast it
    fn elem<E: JsCast>(&self, id: &str) -> Result<E>;
}

impl ElemCast for Document {
    fn elem<E: JsCast>(&self, id: &str) -> Result<E> {
        Ok(self
            .get_element_by_id(id)
            .ok_or("id not found")?
            .dyn_into::<E>()?)
    }
}

/// Fetch a JSON document
async fn fetch_json(window: &Window, uri: &str) -> Result<JsValue> {
    let req = Request::new_with_str(uri)?;
    req.headers().set("Accept", "application/json")?;
    let resp = JsFuture::from(window.fetch_with_request(&req)).await?;
    let resp: Response = resp.dyn_into().unwrap_throw();
    match resp.status() {
        200 => Ok(JsFuture::from(resp.json()?).await?),
        401 => Err(resp.status_text().into()),
        _ => Err(resp.status_text().into()),
    }
}

async fn populate_list(tp: String, tx: String) {
    match tp.as_str() {
        Alarm::ENAME => populate_cards::<Alarm>(tx).await,
        CabinetStyle::ENAME => populate_cards::<CabinetStyle>(tx).await,
        CommConfig::ENAME => populate_cards::<CommConfig>(tx).await,
        CommLink::ENAME => populate_cards::<CommLink>(tx).await,
        Controller::ENAME => populate_cards::<Controller>(tx).await,
        Modem::ENAME => populate_cards::<Modem>(tx).await,
        _ => (),
    }
}

/// Populate cards in `sb_list`
async fn populate_cards<C: Card>(tx: String) {
    if let Err(e) = try_populate_cards::<C>(tx).await {
        // ⛔ 🔒 unauthorized (401) should be handled here
        console::log_1(&e);
    }
}

/// Try to populate cards in `sb_list`
async fn try_populate_cards<C: Card>(tx: String) -> Result<()> {
    let window = web_sys::window().unwrap_throw();
    let doc = window.document().unwrap_throw();
    let sb_list = doc.elem::<Element>("sb_list")?;
    if C::URI.is_empty() {
        sb_list.set_inner_html("");
    } else {
        let json = fetch_json(&window, C::URI).await?;
        let tx = tx.to_lowercase();
        let html = C::build_cards(&json, &tx)?;
        sb_list.set_inner_html(&html);
    }
    Ok(())
}

async fn click_card(tp: String, name: String) {
    match tp.as_str() {
        Alarm::ENAME => expand_card::<Alarm>(name).await,
        CabinetStyle::ENAME => expand_card::<CabinetStyle>(name).await,
        CommConfig::ENAME => expand_card::<CommConfig>(name).await,
        CommLink::ENAME => expand_card::<CommLink>(name).await,
        Controller::ENAME => expand_card::<Controller>(name).await,
        Modem::ENAME => expand_card::<Modem>(name).await,
        _ => (),
    }
}

/// Expand a card to a full form
async fn expand_card<C: Card>(name: String) {
    let window = web_sys::window().unwrap_throw();
    if name.is_empty() {
        // todo: make "new" card?
        return;
    }
    let uri = format!(
        "{}/{}",
        C::URI,
        utf8_percent_encode(&name, NON_ALPHANUMERIC)
    );
    let json = fetch_json(&window, &uri).await.unwrap_throw();
    console::log_1(&json);
    let doc = window.document().unwrap_throw();
    let cs = CardState {
        tname: C::TNAME,
        name,
        json,
    };
    cs.replace_card(&doc, CardType::Any);
    STATE.with(|rc| {
        let mut state = rc.borrow_mut();
        state.selected.replace(cs);
    });
}

#[derive(Clone)]
struct CardState {
    /// Type name
    tname: &'static str,
    /// Object name
    name: String,
    /// JSON value of object
    json: JsValue,
}

impl CardState {
    fn replace_card(&self, doc: &Document, ct: CardType) {
        let id = format!("{}_{}", self.tname, &self.name);
        if let Ok(elem) = doc.elem::<HtmlElement>(&id) {
            match build_card(&self.tname, &self.json, ct) {
                Ok(html) => {
                    elem.set_inner_html(&html);
                    if let CardType::Compact = ct {
                        elem.set_class_name("card");
                    } else {
                        elem.set_class_name("form");
                        let mut opt = ScrollIntoViewOptions::new();
                        opt.behavior(ScrollBehavior::Smooth)
                            .block(ScrollLogicalPosition::Nearest);
                        elem.scroll_into_view_with_scroll_into_view_options(
                            &opt,
                        );
                    }
                }
                Err(e) => {
                    console::log_1(&(&e).into());
                }
            }
        }
    }
}

/// Build card using JSON value
fn build_card(tp: &str, json: &JsValue, ct: CardType) -> Result<String> {
    match tp {
        Alarm::TNAME => Alarm::build_card(json, ct),
        CabinetStyle::TNAME => CabinetStyle::build_card(json, ct),
        CommConfig::TNAME => CommConfig::build_card(json, ct),
        CommLink::TNAME => CommLink::build_card(json, ct),
        Controller::TNAME => Controller::build_card(json, ct),
        Modem::TNAME => Modem::build_card(json, ct),
        _ => Ok("".into()),
    }
}

#[derive(Default)]
struct State {
    selected: Option<CardState>,
}

thread_local! {
    static STATE: RefCell<State> = RefCell::new(State::default());
}

/// Set global allocator to `wee_alloc`
#[global_allocator]
static ALLOC: wee_alloc::WeeAlloc = wee_alloc::WeeAlloc::INIT;

#[wasm_bindgen(start)]
pub async fn start() -> Result<()> {
    // this should be debug only
    console_error_panic_hook::set_once();

    let window = web_sys::window().unwrap_throw();
    let doc = window.document().unwrap_throw();
    let sb_type: HtmlSelectElement = doc.elem("sb_type")?;
    let opt = doc.create_element("option")?;
    opt.append_with_str_1("")?;
    sb_type.append_child(&opt)?;
    let group = doc.create_element("optgroup")?;
    group.set_attribute("label", "🧰 Maintenance")?;
    add_option::<Alarm>(&doc, &group)?;
    add_option::<CabinetStyle>(&doc, &group)?;
    add_option::<CommConfig>(&doc, &group)?;
    add_option::<CommLink>(&doc, &group)?;
    add_option::<Controller>(&doc, &group)?;
    add_option::<Modem>(&doc, &group)?;
    sb_type.append_child(&group)?;
    add_select_event_listener(&sb_type, handle_sb_type_ev)?;
    let sb_input = doc.elem("sb_input")?;
    add_input_event_listener(&sb_input, handle_search_ev)?;
    let sb_list = doc.elem("sb_list")?;
    add_click_event_listener(&sb_list, handle_click_ev)?;
    Ok(())
}

fn add_option<C: Card>(doc: &Document, group: &Element) -> Result<()> {
    let opt = doc.create_element("option")?;
    opt.append_with_str_1(C::ENAME)?;
    group.append_child(&opt)?;
    Ok(())
}

/// Handle an event from "sb_type" `select` element
fn handle_sb_type_ev(tp: String) {
    let window = web_sys::window().unwrap_throw();
    let doc = window.document().unwrap_throw();
    deselect_card(&doc).unwrap_throw();
    let input: HtmlInputElement = doc.elem("sb_input").unwrap_throw();
    input.set_value("");
    spawn_local(populate_list(tp, "".into()));
}

/// Handle an event from "sb_input" `input` element
fn handle_search_ev(tx: String) {
    let window = web_sys::window().unwrap_throw();
    let doc = window.document().unwrap_throw();
    deselect_card(&doc).unwrap_throw();
    let tp = selected_type(&doc).unwrap_throw();
    spawn_local(populate_list(tp, tx));
}

fn selected_type(doc: &Document) -> Result<String> {
    let sb_type: HtmlSelectElement = doc.elem("sb_type")?;
    Ok(sb_type.value())
}

/// Add an "input" event listener to an element
fn add_select_event_listener(
    elem: &HtmlSelectElement,
    handle_ev: fn(String),
) -> Result<()> {
    let closure = Closure::wrap(Box::new(move |e: Event| {
        let value = e
            .current_target()
            .unwrap()
            .dyn_into::<HtmlSelectElement>()
            .unwrap()
            .value();
        handle_ev(value);
    }) as Box<dyn FnMut(_)>);
    elem.add_event_listener_with_callback(
        "input",
        closure.as_ref().unchecked_ref(),
    )?;
    // can't drop closure, just forget it to make JS happy
    closure.forget();
    Ok(())
}

/// Add an "input" event listener to an element
fn add_input_event_listener(
    elem: &HtmlInputElement,
    handle_ev: fn(String),
) -> Result<()> {
    let closure = Closure::wrap(Box::new(move |e: Event| {
        let value = e
            .current_target()
            .unwrap()
            .dyn_into::<HtmlInputElement>()
            .unwrap()
            .value();
        handle_ev(value);
    }) as Box<dyn FnMut(_)>);
    elem.add_event_listener_with_callback(
        "input",
        closure.as_ref().unchecked_ref(),
    )?;
    // can't drop closure, just forget it to make JS happy
    closure.forget();
    Ok(())
}

/// Add a "click" event listener to an element
fn add_click_event_listener(
    elem: &Element,
    handle_ev: fn(&Element),
) -> Result<()> {
    let closure = Closure::wrap(Box::new(move |e: Event| {
        let value = e.target().unwrap().dyn_into::<Element>().unwrap();
        handle_ev(&value);
    }) as Box<dyn FnMut(_)>);
    elem.add_event_listener_with_callback(
        "click",
        closure.as_ref().unchecked_ref(),
    )?;
    // can't drop closure, just forget it to make JS happy
    closure.forget();
    Ok(())
}

/// Handle an event from "sb_list" `click` element
fn handle_click_ev(elem: &Element) {
    let window = web_sys::window().unwrap_throw();
    let doc = window.document().unwrap_throw();
    let tp = selected_type(&doc).unwrap_throw();
    if elem.is_instance_of::<HtmlButtonElement>() {
        handle_button_click_ev(&doc, elem);
    } else if let Some(card) = elem.closest(".card").unwrap_throw() {
        if let Some(name) = card.get_attribute("name") {
            deselect_card(&doc).unwrap_throw();
            spawn_local(click_card(tp, name));
        }
    }
}

fn handle_button_click_ev(doc: &Document, elem: &Element) {
    match elem.id() {
        id if id == "ob_delete" => todo!(),
        id if id == "ob_edit" => {
            let cs = STATE.with(|rc| rc.borrow().selected.clone());
            if let Some(cs) = cs {
                cs.replace_card(&doc, CardType::Edit);
            }
        }
        id if id == "ob_status" => {
            let cs = STATE.with(|rc| rc.borrow().selected.clone());
            if let Some(cs) = cs {
                cs.replace_card(&doc, CardType::Status);
            }
        }
        id if id == "ob_save" => todo!(),
        id => console::log_1(&id.into()),
    }
}

fn deselect_card(doc: &Document) -> Result<()> {
    let cs = STATE.with(|rc| {
        let mut state = rc.borrow_mut();
        state.selected.take()
    });
    if let Some(cs) = cs {
        cs.replace_card(doc, CardType::Compact);
    }
    Ok(())
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn html() {
        assert_eq!(HtmlStr("<").to_string(), "&lt;");
        assert_eq!(HtmlStr(">").to_string(), "&gt;");
        assert_eq!(HtmlStr("&").to_string(), "&amp;");
        assert_eq!(HtmlStr("\"").to_string(), "&quot;");
        assert_eq!(HtmlStr("'").to_string(), "&#x27;");
        assert_eq!(
            HtmlStr("<script>XSS stuff</script>").to_string(),
            "&lt;script&gt;XSS stuff&lt;/script&gt;"
        );
    }
}
