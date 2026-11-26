//! The Rust port of `portables/fullscreen/com/guocheng/mediapocket/fullscreen/`.

#![forbid(unsafe_code)]

const MODES: [&str; 3] = ["smart", "fit", "crop"];
const MIN: f32 = 0.5;
const MAX: f32 = 3.0;
const SMART_CROP: f32 = 0.12;
const SMART_STRETCH: f32 = 0.10;
const MIN_SPAN: f32 = 40.0;
const TABLET_SW: i32 = 600;

fn as_i32(v: i64) -> i32 {
    v as i32
}

fn f32_of(v: f64) -> f32 {
    v as f32
}

/// Port of `mode`.
pub fn mode(saved: String, _migrated: bool) -> Result<String, String> {
    for m in MODES {
        if m == saved {
            return Ok(m.to_string());
        }
    }
    Ok("smart".to_string())
}

/// Port of `tabletStretch`.
pub fn tablet_stretch(mode: String, smallest_width_dp: i64) -> Result<bool, String> {
    Ok(mode == "smart" && tablet(smallest_width_dp)?)
}

/// Port of `stretchFill`.
pub fn stretch_fill(
    screen_w: i64,
    screen_h: i64,
    video_w: i64,
    video_h: i64,
) -> Result<Vec<f64>, String> {
    let fit = box_default(screen_w, screen_h, video_w, video_h, "fit".into())?;
    if fit[0] <= 0 || fit[1] <= 0 {
        return Ok(vec![1.0, 1.0]);
    }
    Ok(vec![
        (as_i32(screen_w) as f32 / fit[0] as f32) as f64,
        (as_i32(screen_h) as f32 / fit[1] as f32) as f64,
    ])
}

/// Port of `smartDefault`.
pub fn smart_default(
    screen_w: i64,
    screen_h: i64,
    video_w: i64,
    video_h: i64,
) -> Result<Vec<f64>, String> {
    smart(screen_w, screen_h, video_w, video_h, 0)
}

/// Port of `smart`.
pub fn smart(
    screen_w: i64,
    screen_h: i64,
    video_w: i64,
    video_h: i64,
    smallest_width_dp: i64,
) -> Result<Vec<f64>, String> {
    if screen_w <= 0 || screen_h <= 0 || video_w <= 0 || video_h <= 0 {
        return Ok(vec![1.0, 1.0]);
    }
    if tablet_stretch("smart".into(), smallest_width_dp)? {
        return stretch_fill(screen_w, screen_h, video_w, video_h);
    }
    let s = screen_w as f64 / screen_h as f64;
    let v = video_w as f64 / video_h as f64;
    if (s / v - 1.0).abs() < 0.005 {
        return Ok(vec![1.0, 1.0]);
    }
    let narrower = v < s;
    let need = if narrower { s / v } else { v / s };
    let u = need.min(1.0 / (1.0 - SMART_CROP as f64));
    let rest = need / u;
    if rest > 1.0 + SMART_STRETCH as f64 + 1e-6 {
        return Ok(vec![1.0, 1.0]);
    }
    if narrower {
        Ok(vec![need as f32 as f64, u as f32 as f64])
    } else {
        Ok(vec![u as f32 as f64, need as f32 as f64])
    }
}

/// Port of `zoom`.
pub fn zoom(_mode: String, saved: f64) -> Result<f64, String> {
    Ok(saved)
}

/// Port of `layoutMode`.
pub fn layout_mode(mode: String) -> Result<String, String> {
    Ok(if mode == "crop" {
        "crop".into()
    } else {
        "fit".into()
    })
}

/// Port of `next`.
pub fn next(mode: String) -> Result<String, String> {
    let i = index(mode)? as usize;
    Ok(MODES[(i + 1) % MODES.len()].to_string())
}

/// Port of `quickNext`.
pub fn quick_next(mode: String) -> Result<String, String> {
    next(mode)
}

/// Port of `cycleHud`. Java always returns `null`.
pub fn cycle_hud(_label: String) -> Result<Option<String>, String> {
    Ok(None)
}

/// Port of `lookDefault`.
pub fn look_default(
    mode: String,
    screen_w: i64,
    screen_h: i64,
    video_w: i64,
    video_h: i64,
) -> Result<Vec<f64>, String> {
    look(mode, screen_w, screen_h, video_w, video_h, 0)
}

/// Port of `look`.
pub fn look(
    mode: String,
    screen_w: i64,
    screen_h: i64,
    video_w: i64,
    video_h: i64,
    smallest_width_dp: i64,
) -> Result<Vec<f64>, String> {
    if mode == "smart" {
        return smart(screen_w, screen_h, video_w, video_h, smallest_width_dp);
    }
    if mode == "crop" && screen_w > 0 && screen_h > 0 && video_w > 0 && video_h > 0 {
        let s = screen_w as f64 / screen_h as f64;
        let v = video_w as f64 / video_h as f64;
        let k = (s / v).max(v / s) as f32 as f64;
        return Ok(vec![k, k]);
    }
    Ok(vec![1.0, 1.0])
}

/// Port of `displayWidth`.
pub fn display_width(width: i64, sar_num: i64, sar_den: i64) -> Result<i64, String> {
    let width = as_i32(width);
    let sar_num = as_i32(sar_num);
    let sar_den = as_i32(sar_den);
    if width <= 0 {
        return Ok(0);
    }
    if sar_num > 0 && sar_den > 0 {
        Ok(((width as i64) * (sar_num as i64) / (sar_den as i64)) as i32 as i64)
    } else {
        Ok(width as i64)
    }
}

fn index_oob(index: usize, len: usize) -> String {
    format!("ArrayIndexOutOfBoundsException: Index {index} out of bounds for length {len}")
}

fn get_f32(v: &[f64], index: usize) -> Result<f32, String> {
    v.get(index)
        .copied()
        .map(f32_of)
        .ok_or_else(|| index_oob(index, v.len()))
}

/// Port of `sameLook`.
///
/// NaN/Infinity on the first axis comparison short-circuits to false (Java `&&`) and must
/// not evaluate `a[1]`/`b[1]` (which can be out of range when the first axis already fails).
pub fn same_look(a: Vec<f64>, b: Vec<f64>) -> Result<bool, String> {
    let a0 = get_f32(&a, 0)?;
    let b0 = get_f32(&b, 0)?;
    let first = (a0 / b0 - 1.0).abs() < 0.03;
    if !first {
        return Ok(false);
    }
    let a1 = get_f32(&a, 1)?;
    let b1 = get_f32(&b, 1)?;
    Ok((a1 / b1 - 1.0).abs() < 0.03)
}

/// Port of `visibleNextDefault`.
pub fn visible_next_default(
    mode: String,
    screen_w: i64,
    screen_h: i64,
    video_w: i64,
    video_h: i64,
) -> Result<String, String> {
    visible_next(mode, screen_w, screen_h, video_w, video_h, 0)
}

/// Port of `visibleNext`.
pub fn visible_next(
    mode_in: String,
    screen_w: i64,
    screen_h: i64,
    video_w: i64,
    video_h: i64,
    smallest_width_dp: i64,
) -> Result<String, String> {
    let cur = mode(mode_in, true)?;
    let now = look(
        cur.clone(),
        screen_w,
        screen_h,
        video_w,
        video_h,
        smallest_width_dp,
    )?;
    let mut nxt = next(cur.clone())?;
    for _ in 0..(MODES.len() - 1) {
        let candidate = look(
            nxt.clone(),
            screen_w,
            screen_h,
            video_w,
            video_h,
            smallest_width_dp,
        )?;
        if !same_look(candidate, now.clone())? {
            return Ok(nxt);
        }
        nxt = next(nxt)?;
    }
    next(cur)
}

/// Port of `index`.
pub fn index(mode: String) -> Result<i64, String> {
    for (i, m) in MODES.iter().enumerate() {
        if *m == mode {
            return Ok(i as i64);
        }
    }
    Ok(0)
}

/// Port of `clamp`.
pub fn clamp(v: f64) -> Result<f64, String> {
    let v = f32_of(v);
    if v.is_nan() || v.is_infinite() {
        return Ok(1.0);
    }
    Ok(v.clamp(MIN, MAX) as f64)
}

/// Port of `originalScale`.
pub fn original_scale(fit_width: i64, video_width: i64) -> Result<f64, String> {
    if fit_width <= 0 || video_width <= 0 {
        return Ok(1.0);
    }
    Ok((1f32).min(video_width as f32 / fit_width as f32) as f64)
}

/// Port of `axisScale`.
pub fn axis_scale(span_x: f64, prev_x: f64, span_y: f64, prev_y: f64) -> Result<Vec<f64>, String> {
    let span_x = f32_of(span_x);
    let prev_x = f32_of(prev_x);
    let span_y = f32_of(span_y);
    let prev_y = f32_of(prev_y);
    let sx = if prev_x >= MIN_SPAN && span_x >= MIN_SPAN {
        span_x / prev_x
    } else {
        1.0
    };
    let sy = if prev_y >= MIN_SPAN && span_y >= MIN_SPAN {
        span_y / prev_y
    } else {
        1.0
    };
    Ok(vec![sx as f64, sy as f64])
}

/// Port of `custom`.
pub fn custom(x: f64, y: f64) -> Result<bool, String> {
    let x = f32_of(x);
    let y = f32_of(y);
    Ok((x - 1.0).abs() > 0.005 || (y - 1.0).abs() > 0.005)
}

/// Port of `zoomLabel`.
pub fn zoom_label(x: f64, y: f64) -> Result<String, String> {
    if !custom(x, y)? {
        return Ok("1.0x".into());
    }
    let x = f32_of(x);
    let y = f32_of(y);
    if (x - y).abs() < 0.005 {
        Ok(format!("{:.1}x", x))
    } else {
        Ok("自定义".into())
    }
}

/// Port of `pipRatio`.
pub fn pip_ratio(w: i64, h: i64) -> Result<f64, String> {
    if w <= 0 || h <= 0 {
        return Ok((16f32 / 9f32) as f64);
    }
    let r = w as f32 / h as f32;
    let lo = 1.0 / 2.39 + 0.001;
    let hi = 2.39 - 0.001;
    Ok(r.max(lo).min(hi) as f64)
}

/// Port of `surfaceDirty`.
#[allow(clippy::too_many_arguments)]
pub fn surface_dirty(
    pivot_x: f64,
    pivot_y: f64,
    tx: f64,
    ty: f64,
    sx: f64,
    sy: f64,
    want_px: f64,
    want_py: f64,
    want_sx: f64,
    want_sy: f64,
) -> Result<bool, String> {
    // Java float `!=` (exact bit compare after f32 truncation of bridge values).
    let pivot_x = f32_of(pivot_x);
    let pivot_y = f32_of(pivot_y);
    let tx = f32_of(tx);
    let ty = f32_of(ty);
    let sx = f32_of(sx);
    let sy = f32_of(sy);
    let want_px = f32_of(want_px);
    let want_py = f32_of(want_py);
    let want_sx = f32_of(want_sx);
    let want_sy = f32_of(want_sy);
    Ok(pivot_x != want_px
        || pivot_y != want_py
        || tx != 0.0
        || ty != 0.0
        || sx != want_sx
        || sy != want_sy)
}

/// Port of `boxDefault`.
pub fn box_default(
    parent_w: i64,
    parent_h: i64,
    video_w: i64,
    video_h: i64,
    mode: String,
) -> Result<Vec<i64>, String> {
    r#box(parent_w, parent_h, video_w, video_h, mode, 0)
}

/// Port of `box`.
pub fn r#box(
    parent_w: i64,
    parent_h: i64,
    video_w: i64,
    video_h: i64,
    mode: String,
    smallest_width_dp: i64,
) -> Result<Vec<i64>, String> {
    let parent_w = as_i32(parent_w);
    let parent_h = as_i32(parent_h);
    let video_w = as_i32(video_w);
    let video_h = as_i32(video_h);
    if parent_w <= 0 || parent_h <= 0 {
        return Ok(vec![0, 0]);
    }
    if video_w <= 0 || video_h <= 0 || mode == "stretch" {
        return Ok(vec![parent_w as i64, parent_h as i64]);
    }
    if mode == "original" {
        let scale = (1.0f64)
            .min(parent_w as f64 / video_w as f64)
            .min(parent_h as f64 / video_h as f64);
        let w = (video_w as f64 * scale).round() as i64;
        let h = (video_h as f64 * scale).round() as i64;
        return Ok(vec![w.max(1), h.max(1)]);
    }
    if mode == "smart" {
        let fit = box_default(
            parent_w as i64,
            parent_h as i64,
            video_w as i64,
            video_h as i64,
            "fit".into(),
        )?;
        let f = smart(
            parent_w as i64,
            parent_h as i64,
            video_w as i64,
            video_h as i64,
            smallest_width_dp,
        )?;
        // Java: Math.round(float) → int
        let w = (fit[0] as f32 * f32_of(f[0])).round() as i32;
        let h = (fit[1] as f32 * f32_of(f[1])).round() as i32;
        return Ok(vec![w.max(1) as i64, h.max(1) as i64]);
    }
    let ratio = if mode == "16:9" {
        16.0 / 9.0
    } else if mode == "4:3" {
        4.0 / 3.0
    } else {
        video_w as f64 / video_h as f64
    };
    let crop = mode == "crop";
    let mut width = parent_w as f64;
    let mut height = width / ratio;
    if if crop {
        height < parent_h as f64
    } else {
        height > parent_h as f64
    } {
        height = parent_h as f64;
        width = height * ratio;
    }
    Ok(vec![
        (width.round() as i64).max(1),
        (height.round() as i64).max(1),
    ])
}

/// Port of `place`.
#[allow(clippy::too_many_arguments)]
pub fn place(
    parent_w: i64,
    parent_h: i64,
    video_w: i64,
    video_h: i64,
    mode: String,
    smallest_width_dp: i64,
) -> Result<Vec<f64>, String> {
    let stretch = tablet_stretch(mode.clone(), smallest_width_dp)?
        && parent_w > 0
        && parent_h > 0
        && video_w > 0
        && video_h > 0;
    let size = if stretch {
        box_default(parent_w, parent_h, video_w, video_h, "fit".into())?
    } else {
        r#box(
            parent_w,
            parent_h,
            video_w,
            video_h,
            mode.clone(),
            smallest_width_dp,
        )?
    };
    let (sx, sy) = if stretch {
        let f = smart(parent_w, parent_h, video_w, video_h, smallest_width_dp)?;
        (f32_of(f[0]) as f64, f32_of(f[1]) as f64)
    } else {
        (1.0, 1.0)
    };
    Ok(vec![size[0] as f64, size[1] as f64, sx, sy])
}

/// Port of `seek`.
pub fn seek(current: i64, duration: i64, delta: i64) -> Result<i64, String> {
    let current = as_i32(current);
    let duration = as_i32(duration);
    let delta = as_i32(delta);
    let target = current as i64 + delta as i64;
    let hi = if duration > 0 {
        duration as i64
    } else {
        i32::MAX as i64
    };
    Ok(target.max(0).min(hi) as i32 as i64)
}

/// Port of `tablet`.
pub fn tablet(smallest_width_dp: i64) -> Result<bool, String> {
    Ok(as_i32(smallest_width_dp) >= TABLET_SW)
}

/// Port of `immersive`.
pub fn immersive(smallest_width_dp: i64, landscape: bool) -> Result<bool, String> {
    Ok(tablet(smallest_width_dp)? || landscape)
}

/// Port of `reserveStatusBar`.
pub fn reserve_status_bar(smallest_width_dp: i64, portrait: bool) -> Result<bool, String> {
    Ok(portrait && !tablet(smallest_width_dp)?)
}

/// Port of `stage`.
///
/// `statusTop+navBottom` uses Java 32-bit int wrap before comparing to `windowH`.
#[allow(clippy::too_many_arguments)]
pub fn stage(
    window_w: i64,
    window_h: i64,
    status_top: i64,
    nav_bottom: i64,
    smallest_width_dp: i64,
    landscape: bool,
    video_w: i64,
    video_h: i64,
) -> Result<Vec<i64>, String> {
    let window_w = as_i32(window_w);
    let window_h = as_i32(window_h);
    let video_w = as_i32(video_w);
    let video_h = as_i32(video_h);
    if window_w <= 0 || window_h <= 0 {
        return Ok(vec![0, 0, 0, 0]);
    }
    if immersive(smallest_width_dp, landscape)? {
        return Ok(vec![0, 0, window_w as i64, window_h as i64]);
    }
    let mut top = as_i32(status_top).max(0);
    let mut bottom = as_i32(nav_bottom).max(0);
    // Java int addition wraps; compare after wrap.
    if top.wrapping_add(bottom) >= window_h {
        top = 0;
        bottom = 0;
    }
    let avail_w = window_w;
    // windowH - top - bottom also wraps in Java.
    let avail_h = window_h.wrapping_sub(top).wrapping_sub(bottom).max(1);
    let ratio = if video_w > 0 && video_h > 0 {
        video_w as f64 / video_h as f64
    } else {
        16.0 / 9.0
    };
    let mut w = avail_w as f64;
    let mut h = w / ratio;
    if h > avail_h as f64 {
        h = avail_h as f64;
        w = h * ratio;
    }
    let mut iw = (w.round() as i32).max(1);
    let ih = (h.round() as i32).max(1);
    if iw > avail_w {
        iw = avail_w;
    }
    let x = ((avail_w - iw) / 2).max(0);
    let y = top + ((avail_h - ih) / 2).max(0);
    Ok(vec![x as i64, y as i64, iw as i64, ih as i64])
}

/// Port of `chromeEdge`.
pub fn chrome_edge(
    bar_and_cutout: i64,
    cutout_only: i64,
    smallest_width_dp: i64,
    portrait: bool,
) -> Result<i64, String> {
    if reserve_status_bar(smallest_width_dp, portrait)? {
        Ok(as_i32(bar_and_cutout).max(0) as i64)
    } else {
        Ok(as_i32(cutout_only).max(0) as i64)
    }
}

/// Port of `dialogWindow`.
pub fn dialog_window(
    screen_w: i64,
    screen_h: i64,
    window_w: i64,
    window_h: i64,
    multi: bool,
    smallest_width_dp: i64,
) -> Result<Vec<i64>, String> {
    if !tablet(smallest_width_dp)? {
        return Ok(vec![-1, -1]);
    }
    let screen_w = as_i32(screen_w);
    let screen_h = as_i32(screen_h);
    let window_w = as_i32(window_w);
    let window_h = as_i32(window_h);
    if multi {
        return Ok(vec![
            (if window_w > 0 { window_w } else { screen_w }).max(0) as i64,
            (if window_h > 0 { window_h } else { screen_h }).max(0) as i64,
        ]);
    }
    Ok(vec![
        screen_w.max(0).max(window_w.max(0)) as i64,
        screen_h.max(0).max(window_h.max(0)) as i64,
    ])
}

/// Port of `orientation`.
pub fn orientation(multi: bool, smallest_width_dp: i64, desired: i64) -> Result<i64, String> {
    if multi || tablet(smallest_width_dp)? {
        Ok(-1)
    } else {
        Ok(as_i32(desired) as i64)
    }
}

/// Port of `clearOf`.
pub fn clear_of(base_px: i64, cutout_px: i64) -> Result<i64, String> {
    Ok(as_i32(base_px).max(0).max(as_i32(cutout_px).max(0)) as i64)
}
