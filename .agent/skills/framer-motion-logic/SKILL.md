---
name: framer-motion-logic
description: Expert guide for Framer Motion and Motion (v11+). Declarative animation orchestration, variants, gestures (hover/tap/drag), layout animations (layout, layoutId), AnimatePresence transitions, spring physics, scroll-linked animations (useScroll, useTransform), useAnimate programmatic sequences, and 3D card tilt interactions.
---

# Framer Motion & Motion Logic

## When to Use This Skill

Use this skill when:
- Building fluid React UI micro-interactions, responsive button states, modal transitions, and navigation menus.
- Orchestrating multi-element animation sequences with `variants` and `staggerChildren`.
- Implementing smooth layout morphing and shared element transitions across routes/tabs with `layout` and `layoutId`.
- Managing component enter/exit lifecycles in the DOM using `AnimatePresence`.
- Binding animations to scroll position with `useScroll` and `useTransform`.
- Creating realistic spring physics (`stiffness`, `damping`, `mass`) or 3D interactive tilt cards with pointer tracking.
- Programmatically driving imperative animation timelines with `useAnimate`.

---

## Installation & Import

In modern packages (v11+), Motion is published as `motion`:
```bash
npm install motion
# or legacy: npm install framer-motion
```

```javascript
// Modern import:
import { motion, AnimatePresence, useScroll, useTransform, useSpring } from 'motion/react';

// Or standard framer-motion import:
import { motion, AnimatePresence } from 'framer-motion';
```

---

## 1. Fundamentals & Spring Physics

Motion uses physics-based springs by default for organic, natural feel:

```jsx
import { motion } from 'motion/react';

export function InteractiveButton() {
  return (
    <motion.button
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.95 }}
      transition={{
        type: 'spring',
        stiffness: 400,
        damping: 17,
      }}
      className="px-6 py-3 bg-indigo-600 text-white font-medium rounded-xl shadow-lg"
    >
      Click Me
    </motion.button>
  );
}
```

---

## 2. Variants & Staggered Orchestration

Use variants to clean up JSX and orchestrate complex hierarchy animations:

```jsx
import { motion } from 'motion/react';

const listVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
      delayChildren: 0.2,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      type: 'spring',
      stiffness: 300,
      damping: 24,
    },
  },
};

export function FeatureList({ items }) {
  return (
    <motion.ul
      variants={listVariants}
      initial="hidden"
      animate="visible"
      className="space-y-4"
    >
      {items.map((item, idx) => (
        <motion.li key={idx} variants={itemVariants} className="p-4 bg-neutral-900 rounded-lg">
          {item}
        </motion.li>
      ))}
    </motion.ul>
  );
}
```

---

## 3. Layout Animations & Shared Elements (`layoutId`)

Create magical, seamless layout transitions without manual math or absolute positioning hacks:

```jsx
import React, { useState } from 'react';
import { motion } from 'motion/react';

const tabs = ['Overview', 'Features', 'Pricing', 'Docs'];

export function AnimatedTabs() {
  const [activeTab, setActiveTab] = useState(tabs[0]);

  return (
    <div className="flex space-x-2 bg-neutral-900 p-1.5 rounded-xl border border-neutral-800">
      {tabs.map((tab) => (
        <button
          key={tab}
          onClick={() => setActiveTab(tab)}
          className={`relative px-4 py-2 text-sm font-medium transition-colors ${
            activeTab === tab ? 'text-white' : 'text-neutral-400 hover:text-neutral-200'
          }`}
        >
          {activeTab === tab && (
            <motion.div
              layoutId="activePill"
              className="absolute inset-0 bg-indigo-600 rounded-lg -z-10"
              transition={{ type: 'spring', stiffness: 500, damping: 35 }}
            />
          )}
          {tab}
        </button>
      ))}
    </div>
  );
}
```

---

## 4. Exit Animations with `AnimatePresence`

Ensure unmounting components gracefully animate out of the DOM before removal:

```jsx
import { motion, AnimatePresence } from 'motion/react';

export function Modal({ isOpen, onClose, title, children }) {
  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm"
          />

          {/* Dialog */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 15 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 15 }}
            transition={{ type: 'spring', damping: 25, stiffness: 350 }}
            className="relative z-10 w-full max-w-md bg-neutral-900 border border-neutral-800 rounded-2xl p-6 shadow-2xl"
          >
            <h3 className="text-lg font-semibold text-white mb-2">{title}</h3>
            {children}
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
```

---

## 5. Scroll-Linked Animations (`useScroll` & `useTransform`)

```jsx
import React, { useRef } from 'react';
import { motion, useScroll, useTransform, useSpring } from 'motion/react';

export function ScrollHero() {
  const containerRef = useRef(null);

  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ['start start', 'end start'],
  });

  // Smooth scroll interpolation
  const smoothProgress = useSpring(scrollYProgress, { stiffness: 100, damping: 20 });

  const scale = useTransform(smoothProgress, [0, 1], [1, 0.8]);
  const opacity = useTransform(smoothProgress, [0, 0.7], [1, 0]);
  const y = useTransform(smoothProgress, [0, 1], [0, 150]);

  return (
    <div ref={containerRef} className="h-[150vh] relative">
      <motion.div
        style={{ scale, opacity, y }}
        className="sticky top-20 flex flex-col items-center justify-center text-center py-20"
      >
        <h1 className="text-6xl font-bold tracking-tight text-white">
          Elevate Your Digital Experience
        </h1>
      </motion.div>
    </div>
  );
}
```

---

## 6. 3D Interactive Tilt Card (Pointer Tracking)

```jsx
import React from 'react';
import { motion, useMotionValue, useSpring, useTransform } from 'motion/react';

export function TiltCard({ title, subtitle }) {
  const x = useMotionValue(0);
  const y = useMotionValue(0);

  const mouseXSpring = useSpring(x);
  const mouseYSpring = useSpring(y);

  const rotateX = useTransform(mouseYSpring, [-0.5, 0.5], ['17.5deg', '-17.5deg']);
  const rotateY = useTransform(mouseXSpring, [-0.5, 0.5], ['-17.5deg', '17.5deg']);

  function handleMouseMove(e) {
    const rect = e.currentTarget.getBoundingClientRect();
    const width = rect.width;
    const height = rect.height;

    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    x.set(mouseX / width - 0.5);
    y.set(mouseY / height - 0.5);
  }

  function handleMouseLeave() {
    x.set(0);
    y.set(0);
  }

  return (
    <motion.div
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      style={{
        rotateY,
        rotateX,
        transformStyle: 'preserve-3d',
      }}
      className="relative w-80 h-96 rounded-2xl bg-gradient-to-br from-indigo-500/20 to-purple-500/20 border border-white/10 p-6 flex flex-col justify-end"
    >
      <div style={{ transform: 'translateZ(50px)' }} className="transition-transform">
        <h3 className="text-2xl font-bold text-white">{title}</h3>
        <p className="text-sm text-neutral-300 mt-1">{subtitle}</p>
      </div>
    </motion.div>
  );
}
```

---

## Performance & Accessibility Rules

- **Only Animate Composite Properties**: Animate `x`, `y`, `scale`, `rotate`, and `opacity`. Never animate layout triggers like `width`, `height`, `top`, or `margin` directly without the `layout` prop.
- **Respect User Accessibility**: Always honor reduced motion preferences:
  ```jsx
  import { useReducedMotion } from 'motion/react';

  const shouldReduceMotion = useReducedMotion();
  const transition = shouldReduceMotion ? { duration: 0 } : { type: 'spring' };
  ```

---

## Detailed References

- [Complete Motion & Framer Motion API Reference](file:///c:/Users/Lenovo/OneDrive/Desktop/AntiGravity/.agent/skills/framer-motion-logic/references/api_reference.md)
- [Comprehensive Motion Design & Implementation Guide](file:///c:/Users/Lenovo/OneDrive/Desktop/AntiGravity/.agent/skills/framer-motion-logic/references/motion-framer-guide.md)
