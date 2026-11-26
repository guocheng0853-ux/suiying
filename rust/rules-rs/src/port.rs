//! The Rust port of `com/guocheng/mediapocket/rules/`.
//!
//! Mirrors `SpiderComponentRules` (0.1.189 component reason codes).

#![forbid(unsafe_code)]

use md5::{Digest as _, Md5};
use regex::Regex;
use sha1::Sha1;
use sha2::Sha256;
use std::sync::OnceLock;

const MAX_COMPONENT_BYTES: i64 = 16 * 1024 * 1024;

fn class_name_re() -> &'static Regex {
    static RE: OnceLock<Regex> = OnceLock::new();
    RE.get_or_init(|| Regex::new(r"^csp_[A-Za-z_$][A-Za-z0-9_$]*$").expect("classname pattern"))
}

fn md5_hex_re() -> &'static Regex {
    static RE: OnceLock<Regex> = OnceLock::new();
    RE.get_or_init(|| Regex::new(r"^[0-9a-f]{32}$").expect("md5 pattern"))
}

fn to_bytes(values: &[i64]) -> Vec<u8> {
    values.iter().map(|b| *b as u8).collect()
}

fn hex_lower(bytes: impl AsRef<[u8]>) -> String {
    let mut out = String::with_capacity(bytes.as_ref().len() * 2);
    for b in bytes.as_ref() {
        use std::fmt::Write as _;
        let _ = write!(out, "{b:02x}");
    }
    out
}

fn check_failed(code: &str) -> String {
    format!("com.guocheng.mediapocket.rules.CheckFailed: {code}")
}

/// Port of `className`.
pub fn class_name(api: String) -> Result<String, String> {
    if !class_name_re().is_match(&api) {
        return Err(check_failed("classname"));
    }
    Ok(format!("com.github.catvod.spider.{}", &api[4..]))
}

/// Port of `md5`.
pub fn md5(declaration: String) -> Result<String, String> {
    let Some((_, rest)) = declaration.split_once(";md5;") else {
        return Ok(String::new());
    };
    let value = rest.trim().to_lowercase();
    if !md5_hex_re().is_match(&value) {
        return Err(check_failed("md5-format"));
    }
    Ok(value)
}

/// Port of `digest`.
pub fn digest(bytes: Vec<i64>, algorithm: String) -> Result<String, String> {
    let data = to_bytes(&bytes);
    let hex = match algorithm.as_str() {
        "MD5" => {
            let mut h = Md5::new();
            h.update(&data);
            hex_lower(h.finalize())
        }
        "SHA-1" => {
            let mut h = Sha1::new();
            h.update(&data);
            hex_lower(h.finalize())
        }
        "SHA-256" => {
            let mut h = Sha256::new();
            h.update(&data);
            hex_lower(h.finalize())
        }
        other => {
            return Err(format!(
                "IllegalArgumentException: {other} MessageDigest not available"
            ));
        }
    };
    Ok(hex)
}

/// Port of `verify`.
pub fn verify(bytes: Vec<i64>, expected: String) -> Result<(), String> {
    let len = bytes.len() as i64;
    if len == 0 || len > MAX_COMPONENT_BYTES {
        return Err(check_failed("size"));
    }
    if !expected.is_empty() {
        let actual = digest(bytes, "MD5".into())?;
        if actual != expected {
            return Err(check_failed("checksum"));
        }
    }
    Ok(())
}

/// Port of `payloadFault`. `None` matches Java `null`.
pub fn payload_fault(text: Option<String>) -> Result<String, String> {
    let Some(text) = text else {
        return Ok("empty".into());
    };
    // Java String.length / charAt are UTF-16 code units; trim uses char <= ' '.
    let units: Vec<u16> = text.encode_utf16().collect();
    let n16 = units.len();
    let mut i = 0usize;
    let mut j = n16;
    while i < n16 && units[i] <= 0x20 {
        i += 1;
    }
    while j > i && units[j - 1] <= 0x20 {
        j -= 1;
    }
    if i < j {
        return Ok(String::new());
    }
    Ok(format!("empty bytes={n16}"))
}

/// Port of `readAllCapped`.
pub fn read_all_capped(source: Vec<i64>) -> Result<Vec<i64>, String> {
    let mut out: Vec<i64> = Vec::new();
    let chunk = 8192usize;
    let mut offset = 0usize;
    while offset < source.len() {
        let end = (offset + chunk).min(source.len());
        let n = end - offset;
        let seen = out.len() as i64 + n as i64;
        if seen > MAX_COMPONENT_BYTES {
            return Err(check_failed("size"));
        }
        out.extend_from_slice(&source[offset..end]);
        offset = end;
    }
    Ok(out)
}

/// Port of `checkLabel`.
pub fn check_label(code: String, bytes: i64) -> Result<String, String> {
    Ok(match code.as_str() {
        "size" => format!("size bytes={bytes}"),
        "checksum" => "checksum".into(),
        "md5-format" => "md5-format".into(),
        "classname" => "classname".into(),
        "" => "unknown".into(),
        other => other.to_string(),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn class_name_ok() {
        assert_eq!(
            class_name("csp_LibvioGuard".into()).unwrap(),
            "com.github.catvod.spider.LibvioGuard"
        );
    }

    #[test]
    fn empty_payload() {
        assert_eq!(payload_fault(None).unwrap(), "empty");
        assert_eq!(
            payload_fault(Some(" \n\t".into())).unwrap(),
            "empty bytes=3"
        );
    }

    #[test]
    fn md5_fixture() {
        assert_eq!(
            digest(
                b"fixture".iter().map(|b| i64::from(*b)).collect(),
                "MD5".into()
            )
            .unwrap(),
            "4cf9d4f0069fc18fb3fcc0a50dceb852"
        );
    }
}
