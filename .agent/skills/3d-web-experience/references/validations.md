# 3D Web Experience - Validations

## Validation Rulebook

### 1. No 3D Loading Indicator
- **Severity**: High
- **Type**: UX / Performance
- **Rule**: All 3D scenes loading remote assets must have a visual indicator or progress percentage.
- **Remedy**: Wrap with `<Suspense fallback={<Loader />}>` in React Three Fiber, or use `THREE.LoadingManager` in Vanilla Three.js.

---

### 2. No WebGL Fallback
- **Severity**: Medium
- **Type**: Compatibility
- **Rule**: Application must gracefully handle environments without WebGL support or where GPU initialization fails.
- **Remedy**: Check WebGL availability with `WebGL.isWebGLAvailable()` or handle `webglcontextlost`, rendering a static 2D hero image or banner if unsupported.

---

### 3. Uncompressed 3D Models
- **Severity**: Medium
- **Type**: Asset Pipeline
- **Rule**: Production `.glb`/`.gltf` files must not exceed 5MB and should use Draco and WebP/KTX2 compression.
- **Remedy**: Run `@gltf-transform/cli optimize` before deployment.

---

### 4. OrbitControls Scroll Interception
- **Severity**: Medium
- **Type**: UX Ergonomics
- **Rule**: Embedded 3D scenes must not hijack page scroll gestures on desktop or touch devices.
- **Remedy**: Set `enableZoom={false}` on `OrbitControls` or constrain touch actions with CSS `touch-action: pan-y`.

---

### 5. Mobile Device Pixel Ratio (DPR) Throttle
- **Severity**: Medium
- **Type**: Performance
- **Rule**: The Canvas DPR on mobile devices must not exceed 1.5 to prevent GPU thermal throttling and battery drain.
- **Remedy**: Configure `<Canvas dpr={isMobile ? 1 : [1, 2]}>`.
