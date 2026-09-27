# 3D Web Experience - Sharp Edges

## 1. WebGL Context Lost (`webglcontextlost`)

### Summary
The 3D scene suddenly goes black or crashes during execution.

### Severity
High

### Root Causes
- Multiple WebGL canvases created simultaneously.
- Texture memory overflow on mobile devices.
- System GPU context reclaimed by browser on memory pressure.

### Prevention & Recovery
```javascript
const canvas = renderer.domElement;

canvas.addEventListener('webglcontextlost', (event) => {
  event.preventDefault();
  console.warn('WebGL context lost, showing fallback...');
  setShowFallback(true);
});

canvas.addEventListener('webglcontextrestored', () => {
  console.log('WebGL context restored, reinitializing scene...');
  initScene();
  setShowFallback(false);
});
```

### React Unmount Cleanup
```jsx
useEffect(() => {
  return () => {
    scene.traverse((object) => {
      if (object.geometry) object.geometry.dispose();
      if (object.material) {
        if (Array.isArray(object.material)) {
          object.material.forEach((m) => m.dispose());
        } else {
          object.material.dispose();
        }
      }
    });
  };
}, [scene]);
```

---

## 2. Huge Model Files & Slow First Render

### Summary
3D models exceed 10MB–30MB, taking seconds to load and causing significant bounce rates.

### Severity
High

### Root Causes
- Direct raw exports from Blender or CAD with uncompressed geometry.
- 4K or 8K textures that exceed mobile GPU limits.
- Redundant embedded materials.

### Fix
```bash
# Compress geometry and convert textures to WebP
npx @gltf-transform/cli optimize input.glb optimized.glb \
  --compress draco \
  --texture-resize 1024 \
  --texture-compress webp
```

---

## 3. Custom Shader Failure on Mobile Devices

### Summary
GLSL custom shaders work in desktop Chrome but crash, throw compile errors, or show magenta/pink materials on iOS Safari or Android.

### Severity
Medium

### Root Causes
- Missing float precision declaration.
- Using `highp` precision in fragment shaders on older mobile GPUs.
- Reliance on non-standard GLSL extensions.

### Safe Shader Template
```glsl
// Always specify precision at the top of fragment shaders
precision mediump float;

varying vec2 vUv;
uniform float uTime;

void main() {
  gl_FragColor = vec4(vUv, sin(uTime) * 0.5 + 0.5, 1.0);
}
```

---

## 4. OrbitControls Hijacking Page Scroll

### Summary
Users try to scroll down a web page with a mouse wheel or touch swipe, but their input gets trapped in the 3D canvas, zooming the model instead of scrolling the page.

### Severity
Medium

### Fix
```jsx
// Disable zoom and pan if the canvas is within a standard scrolling document
<OrbitControls
  enableZoom={false}
  enablePan={false}
  maxPolarAngle={Math.PI / 2}
/>
```
Or only activate controls when the user clicks or toggles an "Interact in 3D" button.
