"""Tests for the Ambiance transfer method."""
from __future__ import annotations

import numpy as np
import pytest

from stealzalut.core.ambiance import fit_ambiance, reference_tint_curve, AmbianceTransferModel
from stealzalut.core.color_transfer import fit_transfer
from stealzalut.core.lut_engine import compute_lut


def test_reference_tint_curve():
    # Constant magenta reference
    ref = np.zeros((32, 32, 3), dtype=np.uint8)
    ref[:, :, 0] = 200
    ref[:, :, 2] = 200
    curve = reference_tint_curve(ref)
    assert curve.shape == (64, 2)
    # Magenta in Oklab has positive a, negative b
    assert np.all(curve[:, 0] > 0)


def test_fit_ambiance_model():
    np.random.seed(42)
    src = np.random.randint(0, 256, (64, 64, 3), dtype=np.uint8)
    ref = np.zeros((64, 64, 3), dtype=np.uint8)
    ref[:, :, 0] = 180
    ref[:, :, 2] = 220

    model = fit_transfer(src, ref, method="ambiance")
    assert isinstance(model, AmbianceTransferModel)
    assert model.method == "ambiance"

    # Test apply on RGB float64 [0, 1]
    colors = np.array([[0.5, 0.5, 0.5], [0.1, 0.1, 0.1], [0.9, 0.9, 0.9]], dtype=np.float64)
    out = model.apply(colors)
    assert out.shape == colors.shape
    assert np.all((out >= 0.0) & (out <= 1.0))


def test_ambiance_compute_lut():
    src = np.full((32, 32, 3), 128, dtype=np.uint8)
    ref = np.full((32, 32, 3), 64, dtype=np.uint8)
    ref[:, :, 0] = 220  # Reddish cast

    lut = compute_lut(src, ref, method="ambiance", size=17)
    assert lut.shape == (17 * 17 * 17, 3)
    assert np.all(lut >= 0.0) and np.all(lut <= 1.0)
