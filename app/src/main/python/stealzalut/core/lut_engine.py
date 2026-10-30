"""3-D LUT generation engine.

Builds an identity lattice, pushes it through the colour-transfer
pipeline as a pseudo-image, and returns the transformed lookup table
ready for export.
"""

from __future__ import annotations

import numpy as np
from numpy.typing import NDArray

from stealzalut.utils.image_io import to_float64, to_uint8


def generate_identity_lattice(size: int = 33) -> NDArray[np.float64]:
    """Create a 3-D identity LUT lattice.

    The lattice contains ``size ** 3`` RGB triplets whose values span
    linearly from 0.0 to 1.0.  The ordering follows the Adobe ``.cube``
    convention: **R varies fastest**, then G, then B.

    Args:
        size: Number of grid points per channel axis.

    Returns:
        ``float64`` array of shape ``(size ** 3, 3)``.
    """
    # linspace for one axis
    axis: NDArray[np.float64] = np.linspace(0.0, 1.0, size, dtype=np.float64)

    # Build meshgrid with B as the slowest (outermost) axis and R as the
    # fastest (innermost).  With ``indexing='ij'`` and argument order
    # (B, G, R), C-order flattening yields R-fastest iteration, which is
    # the Adobe .cube convention.
    b, g, r = np.meshgrid(axis, axis, axis, indexing="ij")

    lattice: NDArray[np.float64] = np.stack(
        [r.ravel(), g.ravel(), b.ravel()], axis=-1
    )
    return lattice


def compute_lut(
    src_img: NDArray[np.uint8],
    ref_img: NDArray[np.uint8],
    size: int = 33,
    method: str = "hm-mkl-hm",
) -> NDArray[np.float64]:
    """Compute a 3-D colour LUT by colour-transferring an identity lattice.

    The identity lattice is reshaped into a tall pseudo-image of shape
    ``(size ** 3, 1, 3)`` so that the existing colour-transfer function
    can be applied directly.

    Args:
        src_img: Source image (RGB ``uint8``, ``(H, W, 3)``).
        ref_img: Reference image (RGB ``uint8``, ``(H, W, 3)``).
        size: LUT grid size per channel.
        method: Colour-transfer algorithm key.

    Returns:
        Transformed LUT as a ``float64`` array of shape
        ``(size ** 3, 3)`` with values in ``[0.0, 1.0]``.
    """
    identity: NDArray[np.float64] = generate_identity_lattice(size)

    # Local import to decouple pure lattice math from color_matcher dependency
    from stealzalut.core.color_transfer import compute_color_transfer

    # Convert identity lattice to a pseudo-image (N, 1, 3) uint8
    pseudo_img: NDArray[np.uint8] = to_uint8(identity).reshape(-1, 1, 3)

    # Run colour transfer
    transferred: NDArray[np.uint8] = compute_color_transfer(
        src_img=pseudo_img,
        ref_img=ref_img,
        method=method,
    )

    # Back to float64 (N, 3)
    lut: NDArray[np.float64] = to_float64(
        transferred.reshape(-1, 3)
    )
    return lut


def apply_intensity(
    identity: NDArray[np.float64],
    transformed: NDArray[np.float64],
    intensity: float = 1.0,
) -> NDArray[np.float64]:
    """Blend between the identity and transformed LUT tables.

    Linear interpolation:
    ``result = (1 - intensity) * identity + intensity * transformed``

    Args:
        identity: Identity (neutral) LUT, shape ``(N, 3)``.
        transformed: Colour-transferred LUT, shape ``(N, 3)``.
        intensity: Blend factor in ``[0.0, 1.0]``.  ``0.0`` returns
            the identity; ``1.0`` returns the full transfer.

    Returns:
        Blended ``float64`` array of shape ``(N, 3)`` clipped to
        ``[0.0, 1.0]``.
    """
    result: NDArray[np.float64] = (
        (1.0 - intensity) * identity + intensity * transformed
    )
    return np.clip(result, 0.0, 1.0)
