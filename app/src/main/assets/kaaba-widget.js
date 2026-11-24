/**
 * KaabaWidget — مجسم الكعبة ثلاثي الأبعاد المطور (Realistic 3D)
 * تم التعديل جراحياً ليتناسب مع تصميم المصحف الكوني بناءً على مقترح المستخدم
 */
(function (global) {
  var STYLE_ID = 'kaaba-widget-style';

  var CSS = [
    '.kw-scene { perspective: 1000px; display: flex; align-items: center; justify-content: center; pointer-events: none; }',
    '.kw-cube { position: relative; width: 0; height: 0; transform-style: preserve-3d; transition: transform 0.1s linear; }',
    '.kw-face { position: absolute; left: 0; top: 0; backface-visibility: hidden; box-sizing: border-box; border: 0.1px solid rgba(255,255,255,0.05);',
    '  background:',
    '   linear-gradient(to bottom, transparent 0 17%, #d9b25a 17% 19%, #8a6a1f 19% 20%, #d9b25a 20% 31%, #8a6a1f 31% 32%, #d9b25a 32% 34%, transparent 34%),',
    '   linear-gradient(var(--c1), var(--c2)) }',
    '.kw-front, .kw-back { width: var(--W); height: var(--H); margin: calc(var(--H)/-2) 0 0 calc(var(--W)/-2); }',
    '.kw-left, .kw-right { width: var(--D); height: var(--H); margin: calc(var(--H)/-2) 0 0 calc(var(--D)/-2); }',
    '.kw-top { width: var(--W); height: var(--D); margin: calc(var(--D)/-2) 0 0 calc(var(--W)/-2); background: #1a1a1a; box-shadow: inset 0 0 15px #000; }',
    '.kw-front { --c1: #262626; --c2: #111; transform: translateZ(calc(var(--D)/2)); }',
    '.kw-back { --c1: #111; --c2: #1b1b1b; transform: rotateY(180deg) translateZ(calc(var(--D)/2)); }',
    '.kw-right { --c1: #1b1b1b; --c2: #111; transform: rotateY(90deg) translateZ(calc(var(--W)/2)); }',
    '.kw-left { --c1: #262626; --c2: #1b1b1b; transform: rotateY(-90deg) translateZ(calc(var(--W)/2)); }',
    '.kw-top { transform: rotateX(90deg) translateZ(calc(var(--H)/2)); }',
    '.kw-door { position: absolute; left: 60%; bottom: 8%; width: 26%; height: 42%; background: linear-gradient(145deg, #f3d078, #b88a2c 55%, #7a5a14); border: 0.5px solid #f6e3a1; border-bottom: 0; border-radius: 2px 2px 0 0; box-shadow: 1px 1px 5px rgba(0,0,0,0.5); z-index: 5; }'
  ].join('\n');

  function injectStyle() {
    if (document.getElementById(STYLE_ID)) return;
    var s = document.createElement('style');
    s.id = STYLE_ID;
    s.textContent = CSS;
    document.head.appendChild(s);
  }

  function mount(container, opts) {
    opts = opts || {};
    var size = opts.size || 52;
    injectStyle();

    var scene = document.createElement('div');
    scene.className = 'kw-scene';
    scene.style.setProperty('--H', size + 'px');
    scene.style.setProperty('--W', (size * 0.9) + 'px');
    scene.style.setProperty('--D', (size * 0.8) + 'px');

    scene.innerHTML =
      '<div class="kw-cube">' +
        '<div class="kw-face kw-front"><div class="kw-door"></div></div>' +
        '<div class="kw-face kw-back"></div>' +
        '<div class="kw-face kw-left"></div>' +
        '<div class="kw-face kw-right"></div>' +
        '<div class="kw-face kw-top"></div>' +
      '</div>';

    container.appendChild(scene);
    var cube = scene.firstChild;

    var api = {
      setAngle: function (ry, rx) {
        var x = rx || -18;
        cube.style.transform = 'rotateX(' + x + 'deg) rotateY(' + ry + 'deg)';
        return api;
      },
      destroy: function () {
        if (scene.parentNode) scene.parentNode.removeChild(scene);
      }
    };
    return api;
  }

  global.KaabaWidget = { mount: mount };
})(window);
