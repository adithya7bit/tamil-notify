---
name: glsl-shaders-custom-materials
description: Expert guide for writing GLSL shaders (Vertex/Fragment) and building custom materials in Three.js, React Three Fiber, and WebGL. Covers ShaderMaterial, RawShaderMaterial, uniforms, varyings, attributes, procedural noise, PBR extensions via onBeforeCompile, Fresnel effects, and mobile GPU optimization.
---

# GLSL Shaders & Custom Materials

## When to Use This Skill

Use this skill when:
- Creating custom visual effects (liquid distortion, holographic scanlines, procedural terrain, dissolve effects, energy shields).
- Writing GPU-accelerated vertex shaders (displacements, wave animations, particle systems).
- Writing fragment shaders (procedural patterns, noise, lighting models, color grading, chromatic aberration).
- Extending Three.js built-in materials (PBR `MeshStandardMaterial` / `MeshPhysicalMaterial`) using `onBeforeCompile`.
- Integrating shaders into React Three Fiber using `shaderMaterial` from `@react-three/drei`.
- Optimizing GPU memory, reducing draw calls, and ensuring cross-device mobile shader compatibility.

---

## Core Pipeline Architecture

Shaders run directly on the GPU in two main programmable stages:

1. **Vertex Shader**: Executes once per vertex. Computes screen projection (`gl_Position`), modifies vertex positions (wave displacement), and passes data (`varying`) to the fragment shader.
2. **Fragment (Pixel) Shader**: Executes once per rasterized pixel. Computes the final RGBA color (`gl_FragColor`).

### Data Communication
- **Attributes**: Per-vertex data (positions, UVs, normals). Input to vertex shader only.
- **Uniforms**: Global variables constant across all vertices/pixels (time, resolution, color, textures).
- **Varyings**: Interpolated values passed from the vertex shader to the fragment shader.

```
CPU (JavaScript) ──[ Uniforms ]──> Vertex Shader ──[ Varyings ]──> Fragment Shader ──> Screen
CPU (JavaScript) ──[ Attributes ]─┘
```

---

## 1. Quick Start: Vanilla Three.js `ShaderMaterial`

```javascript
import * as THREE from 'three';

const customMaterial = new THREE.ShaderMaterial({
  uniforms: {
    uTime: { value: 0 },
    uColor: { value: new THREE.Color(0x6366f1) },
    uTexture: { value: null }
  },
  vertexShader: `
    varying vec2 vUv;
    varying vec3 vNormal;

    void main() {
      vUv = uv;
      vNormal = normalize(normalMatrix * normal);
      gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
  `,
  fragmentShader: `
    precision mediump float;

    uniform float uTime;
    uniform vec3 uColor;
    varying vec2 vUv;
    varying vec3 vNormal;

    void main() {
      // Subtle pulse and gradient
      float pulse = sin(uTime * 2.0 + vUv.x * 6.28) * 0.5 + 0.5;
      vec3 finalColor = mix(uColor, vec3(1.0), pulse * 0.3);
      gl_FragColor = vec4(finalColor, 1.0);
    }
  `,
  transparent: false,
  side: THREE.DoubleSide
});

// Update in render loop:
function animate(time) {
  customMaterial.uniforms.uTime.value = time * 0.001;
  renderer.render(scene, camera);
}
```

---

## 2. React Three Fiber: `shaderMaterial` (Drei)

Declare and register custom shaders declaratively in R3F:

```jsx
import React, { useRef } from 'react';
import { extend, useFrame } from '@react-three/fiber';
import { shaderMaterial } from '@react-three/drei';
import * as THREE from 'three';

// 1. Define the material
const WaveMaterial = shaderMaterial(
  {
    uTime: 0,
    uColorStart: new THREE.Color('#4338ca'),
    uColorEnd: new THREE.Color('#06b6d4'),
  },
  // Vertex Shader
  `
    varying vec2 vUv;
    uniform float uTime;

    void main() {
      vUv = uv;
      vec3 pos = position;
      pos.z += sin(pos.x * 3.0 + uTime) * 0.2;
      pos.z += cos(pos.y * 2.0 + uTime * 1.5) * 0.15;
      gl_Position = projectionMatrix * modelViewMatrix * vec4(pos, 1.0);
    }
  `,
  // Fragment Shader
  `
    precision mediump float;
    varying vec2 vUv;
    uniform vec3 uColorStart;
    uniform vec3 uColorEnd;

    void main() {
      vec3 color = mix(uColorStart, uColorEnd, vUv.y);
      gl_FragColor = vec4(color, 1.0);
    }
  `
);

// 2. Register with R3F JSX
extend({ WaveMaterial });

// 3. Use in component
export function WavePlane() {
  const materialRef = useRef();

  useFrame((_, delta) => {
    if (materialRef.current) {
      materialRef.current.uTime += delta;
    }
  });

  return (
    <mesh rotation={[-Math.PI / 3, 0, 0]}>
      <planeGeometry args={[4, 4, 64, 64]} />
      <waveMaterial ref={materialRef} wireframe={false} />
    </mesh>
  );
}
```

---

## 3. High-Impact Shader Recipes

### A. Fresnel Effect (Hologram / Force Field Rim Glow)
```glsl
// Fragment Shader
precision mediump float;
varying vec3 vNormal;
varying vec3 vViewPosition; // From vertex: vViewPosition = -(modelViewMatrix * vec4(position, 1.0)).xyz;
uniform vec3 uGlowColor;

void main() {
  vec3 viewDir = normalize(vViewPosition);
  float fresnel = 1.0 - max(dot(viewDir, normalize(vNormal)), 0.0);
  fresnel = pow(fresnel, 2.5); // Adjust glow sharpness

  gl_FragColor = vec4(uGlowColor * fresnel, fresnel);
}
```

### B. Dissolve / Noise Cutout Shader
```glsl
// Fragment Shader
precision mediump float;
varying vec2 vUv;
uniform float uProgress; // 0.0 to 1.0
uniform sampler2D uNoiseTexture;

void main() {
  float noise = texture2D(uNoiseTexture, vUv).r;
  
  if (noise < uProgress) {
    discard; // Cut out pixel
  }

  // Edge burn glow
  float edge = smoothstep(uProgress, uProgress + 0.05, noise);
  vec3 baseColor = vec3(0.1, 0.1, 0.1);
  vec3 edgeColor = vec3(1.0, 0.4, 0.1); // Fiery orange edge
  vec3 finalColor = mix(edgeColor, baseColor, edge);

  gl_FragColor = vec4(finalColor, 1.0);
}
```

---

## 4. Extending Built-in PBR Materials (`onBeforeCompile`)

Keep realistic PBR lighting, shadows, and environment reflections while modifying vertex displacement or surface details:

```javascript
const material = new THREE.MeshStandardMaterial({
  color: 0x22c55e,
  roughness: 0.3,
  metalness: 0.8
});

const customUniforms = {
  uTime: { value: 0 }
};

material.onBeforeCompile = (shader) => {
  shader.uniforms.uTime = customUniforms.uTime;

  // 1. Inject uniforms and custom vertex displacement
  shader.vertexShader = `
    uniform float uTime;
    ${shader.vertexShader}
  `.replace(
    '#include <begin_vertex>',
    `
    #include <begin_vertex>
    transformed.y += sin(position.x * 5.0 + uTime * 2.0) * 0.15;
    `
  );
};
```

---

## 5. Mobile & GPU Optimization Rules

- **Declare Precision Explicitly**: Always place `precision mediump float;` at the top of fragment shaders to prevent mobile WebGL crashes.
- **Avoid Dynamic Branching**: Conditionals (`if/else`) inside fragment shaders can serialize GPU execution pipelines. Use `step()`, `smoothstep()`, `clamp()`, and `mix()` instead:
  ```glsl
  // Avoid:
  // if (vUv.x > 0.5) color = a; else color = b;

  // Good:
  color = mix(a, b, step(0.5, vUv.x));
  ```
- **Disposal on Unmount**: When destroying meshes, explicitly invoke `.dispose()` on geometries, materials, and textures to release GPU memory allocations.

---

## Related References

For full material inventories and deep shader chunks:
- [Three.js Shaders Reference](file:///c:/Users/Lenovo/OneDrive/Desktop/AntiGravity/.agent/skills/glsl-shaders-custom-materials/references/threejs-shaders.md)
- [Three.js Materials Catalog](file:///c:/Users/Lenovo/OneDrive/Desktop/AntiGravity/.agent/skills/glsl-shaders-custom-materials/references/threejs-materials.md)
