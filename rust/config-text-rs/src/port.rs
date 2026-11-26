//! Rust port of `com.guocheng.mediapocket.config.ConfigTextRules`.

#![forbid(unsafe_code)]

use base64::Engine as _;
use base64::alphabet;
use base64::engine::{DecodePaddingMode, GeneralPurpose, GeneralPurposeConfig};

/// Match Java `Base64.getDecoder()`: allow non-canonical trailing bits.
const JAVA_B64: GeneralPurpose = GeneralPurpose::new(
    &alphabet::STANDARD,
    GeneralPurposeConfig::new()
        .with_decode_allow_trailing_bits(true)
        .with_decode_padding_mode(DecodePaddingMode::Indifferent),
);

const MAX_CHARS: usize = 8 * 1024 * 1024;
const MAX_DEPTH: usize = 32;

fn failed(message: &str) -> String {
    format!("com.guocheng.mediapocket.config.ConfigTextFailed: {message}")
}

fn units(text: &str) -> Vec<u16> {
    text.encode_utf16().collect()
}

fn from_units(units: &[u16]) -> String {
    String::from_utf16_lossy(units)
}

fn is_ascii_ws(c: u16) -> bool {
    c == b' ' as u16 || c == b'\n' as u16 || c == b'\r' as u16 || c == b'\t' as u16
}

fn alnum(c: u16) -> bool {
    (b'A' as u16..=b'Z' as u16).contains(&c)
        || (b'a' as u16..=b'z' as u16).contains(&c)
        || (b'0' as u16..=b'9' as u16).contains(&c)
}

fn ws(c: u16) -> bool {
    is_ascii_ws(c)
}

/// Port of `normalizeBody`.
pub fn normalize_body(raw: Option<String>) -> Result<String, String> {
    let Some(raw) = raw else {
        return Err(failed("配置内容是空的"));
    };
    if raw.is_empty() {
        return Err(failed("配置内容是空的"));
    }
    // Java String.length is UTF-16 length.
    if units(&raw).len() > MAX_CHARS {
        return Err(failed("配置过大"));
    }
    let text = bom(java_trim(&raw))?;
    if text.is_empty() {
        return Err(failed("配置内容是空的"));
    }
    if base64(text.clone())? {
        let decoded = decode(text)?;
        if units(&decoded).len() > MAX_CHARS {
            return Err(failed("配置过大"));
        }
        return prepared_json(decoded);
    }
    prepared_json(text)
}

/// Port of `bom`. Java only trims when a BOM was actually stripped.
pub fn bom(text: String) -> Result<String, String> {
    let u = units(&text);
    let mut i = 0;
    while i < u.len() && u[i] == 0xfeff {
        i += 1;
    }
    if i == 0 {
        return Ok(text);
    }
    // Java String.trim(): code units <= U+0020.
    Ok(java_trim(&from_units(&u[i..])))
}

fn java_trim(s: &str) -> String {
    let u = units(s);
    let mut a = 0;
    let mut b = u.len();
    while a < b && u[a] <= 0x20 {
        a += 1;
    }
    while b > a && u[b - 1] <= 0x20 {
        b -= 1;
    }
    from_units(&u[a..b])
}

/// Port of `preparedJson`.
pub fn prepared_json(text: String) -> Result<String, String> {
    let text = java_trim(&strip(bom(text)?)?);
    let u = units(&text);
    if u.is_empty() || u[0] != b'{' as u16 {
        return Err(failed("配置不是合法 JSON"));
    }
    structure(text.clone())?;
    Ok(text)
}

/// Port of `base64`.
pub fn base64(text: String) -> Result<bool, String> {
    let u = units(&text);
    let mut body = 0i32;
    let mut pad = 0i32;
    let mut padding = false;
    let mut url = false;
    let mut std = false;
    for &c in &u {
        if is_ascii_ws(c) {
            continue;
        }
        if c == b'=' as u16 {
            if pad == 2 {
                return Ok(false);
            }
            padding = true;
            pad += 1;
            continue;
        }
        if padding {
            return Ok(false);
        }
        if c == b'+' as u16 || c == b'/' as u16 {
            std = true;
        } else if c == b'-' as u16 || c == b'_' as u16 {
            url = true;
        } else if !alnum(c) {
            return Ok(false);
        }
        body += 1;
    }
    Ok(body > 0 && body % 4 != 1 && !(std && url))
}

/// Port of `decode`.
pub fn decode(text: String) -> Result<String, String> {
    let u = units(&text);
    let mut body: Vec<u16> = Vec::with_capacity(u.len());
    let mut pad = 0i32;
    let mut url = false;
    for &c in &u {
        if is_ascii_ws(c) {
            continue;
        }
        if c == b'=' as u16 {
            pad += 1;
            continue;
        }
        if c == b'-' as u16 || c == b'_' as u16 {
            url = true;
        }
        body.push(c);
    }
    let need = (4 - (body.len() as i32).rem_euclid(4)).rem_euclid(4);
    if pad != 0 && pad != need {
        return Err(failed("配置不是合法 JSON"));
    }
    if url {
        for c in &mut body {
            if *c == b'-' as u16 {
                *c = b'+' as u16;
            } else if *c == b'_' as u16 {
                *c = b'/' as u16;
            }
        }
    }
    for _ in 0..need {
        body.push(b'=' as u16);
    }
    let ascii: String = body
        .iter()
        .map(|&c| char::from_u32(c as u32).unwrap_or('\0'))
        .collect();
    let bytes = match JAVA_B64.decode(ascii.as_bytes()) {
        Ok(b) => b,
        Err(_) => return Err(failed("配置不是合法 JSON")),
    };
    match String::from_utf8(bytes) {
        Ok(s) => Ok(s),
        Err(_) => Err(failed("配置编码无法识别")),
    }
}

/// Port of `strip`.
pub fn strip(text: String) -> Result<String, String> {
    let src = units(&text);
    let mut out: Vec<u16> = Vec::with_capacity(src.len());
    let mut quote: u16 = 0;
    let mut escape = false;
    let n = src.len();
    let mut i = 0;
    while i < n {
        let c = src[i];
        if quote != 0 {
            out.push(c);
            i += 1;
            if escape {
                escape = false;
                continue;
            }
            if c == b'\\' as u16 {
                escape = true;
                continue;
            }
            if c == quote {
                quote = 0;
            }
            continue;
        }
        if c == b'"' as u16 || c == b'\'' as u16 {
            quote = c;
            out.push(c);
            i += 1;
            continue;
        }
        if c == b'#' as u16 {
            i = line_comment(&src, i + 1, &mut out);
            continue;
        }
        if c == b'/' as u16 && i + 1 < n {
            let next = src[i + 1];
            if next == b'/' as u16 {
                i += 2;
                while i < n {
                    let d = src[i];
                    if d == b'\n' as u16 || d == 0x2028 || d == 0x2029 {
                        out.push(d);
                        i += 1;
                        break;
                    }
                    if d == b'\r' as u16 {
                        out.push(b'\n' as u16);
                        i += 1;
                        if i < n && src[i] == b'\n' as u16 {
                            i += 1;
                        }
                        break;
                    }
                    i += 1;
                }
                continue;
            }
            if next == b'*' as u16 {
                let mut j = i + 2;
                while j + 1 < n && !(src[j] == b'*' as u16 && src[j + 1] == b'/' as u16) {
                    j += 1;
                }
                if j + 1 >= n {
                    return Err(failed("配置注释不完整"));
                }
                out.push(b' ' as u16);
                i = j + 2;
                continue;
            }
        }
        out.push(c);
        i += 1;
    }
    if quote != 0 || escape {
        return Err(failed("配置不是合法 JSON"));
    }
    Ok(from_units(&out))
}

fn line_comment(src: &[u16], mut i: usize, out: &mut Vec<u16>) -> usize {
    let n = src.len();
    while i < n {
        let d = src[i];
        if d == b'\n' as u16 || d == 0x2028 || d == 0x2029 {
            out.push(d);
            return i + 1;
        }
        if d == b'\r' as u16 {
            out.push(b'\n' as u16);
            if i + 1 < n && src[i + 1] == b'\n' as u16 {
                return i + 2;
            }
            return i + 1;
        }
        i += 1;
    }
    i
}

/// Port of `structure`.
pub fn structure(text: String) -> Result<(), String> {
    let src = units(&text);
    let n = src.len();
    let mut i = 0;
    while i < n && ws(src[i]) {
        i += 1;
    }
    if i >= n || src[i] != b'{' as u16 {
        return Err(failed("配置不是合法 JSON"));
    }
    let mut stack = [0u16; MAX_DEPTH];
    let mut sp = 0usize;
    let mut quote: u16 = 0;
    let mut escape = false;
    while i < n {
        let c = src[i];
        if quote != 0 {
            if escape {
                escape = false;
                i += 1;
                continue;
            }
            if c == b'\\' as u16 {
                escape = true;
                i += 1;
                continue;
            }
            if c == quote {
                quote = 0;
            }
            i += 1;
            continue;
        }
        if c == b'"' as u16 || c == b'\'' as u16 {
            quote = c;
            i += 1;
            continue;
        }
        if c == b'{' as u16 || c == b'[' as u16 {
            if sp == MAX_DEPTH {
                return Err(failed("配置结构过深"));
            }
            stack[sp] = if c == b'{' as u16 {
                b'}' as u16
            } else {
                b']' as u16
            };
            sp += 1;
            i += 1;
            continue;
        }
        if c == b'}' as u16 || c == b']' as u16 {
            if sp == 0 || stack[sp - 1] != c {
                return Err(failed("配置不是合法 JSON"));
            }
            sp -= 1;
            if sp == 0 {
                return Ok(());
            }
        }
        i += 1;
    }
    Err(failed("配置不是合法 JSON"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn strips_line_comment() {
        let out = normalize_body(Some("{\"a\":1 // comment\n}".into())).unwrap();
        assert_eq!(out, "{\"a\":1 \n}");
    }

    #[test]
    fn empty_raw() {
        let err = normalize_body(Some(String::new())).unwrap_err();
        assert!(err.contains("配置内容是空的"));
    }
}
