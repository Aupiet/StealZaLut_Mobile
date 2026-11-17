//! Rust benchmark implementation for 3D LUT trilinear interpolation & Gaussian blur.
//! Note: Can be compiled natively or cross-compiled to Android via cargo-ndk.

use rayon::prelude::*;
use std::time::Instant;

pub fn apply_lut_trilinear(
    img: &[f32],
    width: usize,
    height: usize,
    lut: &[f32],
    lut_size: usize,
    out: &mut [f32],
) {
    let scale = (lut_size - 1) as f32;
    let n2 = lut_size * lut_size;
    let max_coord = lut_size - 2;

    out.par_chunks_exact_mut(width * 3)
        .enumerate()
        .for_each(|(y, out_row)| {
            let in_row = &img[y * width * 3..(y + 1) * width * 3];
            for x in 0..width {
                let px = x * 3;
                let r = (in_row[px] * scale).clamp(0.0, scale);
                let g = (in_row[px + 1] * scale).clamp(0.0, scale);
                let b = (in_row[px + 2] * scale).clamp(0.0, scale);

                let x0 = (r as usize).min(max_coord);
                let y0 = (g as usize).min(max_coord);
                let z0 = (b as usize).min(max_coord);

                let dx = r - x0 as f32;
                let dy = g - y0 as f32;
                let dz = b - z0 as f32;

                let base = (z0 * n2 + y0 * lut_size + x0) * 3;
                let base_p1 = base + 3;
                let base_pn = (z0 * n2 + (y0 + 1) * lut_size + x0) * 3;
                let base_pn1 = base_pn + 3;
                let base_pn2 = ((z0 + 1) * n2 + y0 * lut_size + x0) * 3;
                let base_pn21 = base_pn2 + 3;
                let base_pn2n = ((z0 + 1) * n2 + (y0 + 1) * lut_size + x0) * 3;
                let base_pn2n1 = base_pn2n + 3;

                let one_dx = 1.0 - dx;
                let one_dy = 1.0 - dy;
                let one_dz = 1.0 - dz;

                for c in 0..3 {
                    let c00 = one_dx * lut[base + c] + dx * lut[base_p1 + c];
                    let c01 = one_dx * lut[base_pn + c] + dx * lut[base_pn1 + c];
                    let c10 = one_dx * lut[base_pn2 + c] + dx * lut[base_pn21 + c];
                    let c11 = one_dx * lut[base_pn2n + c] + dx * lut[base_pn2n1 + c];

                    let c0 = one_dy * c00 + dy * c01;
                    let c1 = one_dy * c10 + dy * c11;

                    let val = (one_dz * c0 + dz * c1).clamp(0.0, 1.0);
                    out_row[px + c] = val;
                }
            }
        });
}

fn main() {
    println!("Rust benchmark ready for cargo build --release");
}
