import{p as J}from"./chunks/host-transport-Bqx2Nq-D.js";import{a as H,p as K}from"./chunks/global-BmEm1LAj.js";import{e as X}from"./chunks/epub-fGgDOzW3.js";import{a as Z,b as _,c as G}from"./chunks/reader-document-vars-mLGELDNS.js";import{r as M,p as P,a as k}from"./chunks/toc-host-events-Z4ZhGZnt.js";import{p as I}from"./chunks/reader-metrics-host-DEnNtJWi.js";import"./chunks/_commonjs-dynamic-modules-TDtrdbi3.js";function g(e){return new URL(`fonts/${e}`,window.location.href).href}function ee(){return`
@font-face {
  font-family: "Quicksand";
  src: url("${g("Quicksand-Light.ttf")}") format("truetype");
  font-weight: 300;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: "Quicksand";
  src: url("${g("Quicksand-Regular.ttf")}") format("truetype");
  font-weight: 400;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: "Quicksand";
  src: url("${g("Quicksand-Medium.ttf")}") format("truetype");
  font-weight: 500;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: "Quicksand";
  src: url("${g("Quicksand-SemiBold.ttf")}") format("truetype");
  font-weight: 600;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: "Quicksand";
  src: url("${g("Quicksand-Bold.ttf")}") format("truetype");
  font-weight: 700;
  font-style: normal;
  font-display: swap;
}
`}function te(){return`
${ee()}

html {
  -webkit-text-size-adjust: 100%;
  overflow-wrap: anywhere;
  word-wrap: break-word;
  -webkit-user-select: text;
  user-select: text;
}

html, body {
  font-family: var(--yaba-font-family, "Quicksand", -apple-system, BlinkMacSystemFont, sans-serif) !important;
  font-size: var(--yaba-reader-font-size, 18px) !important;
  line-height: var(--yaba-reader-line-height, 1.6) !important;
  color: var(--yaba-reader-on-bg) !important;
  background: var(--yaba-reader-bg, transparent) !important;
  box-sizing: border-box !important;
  max-width: 100% !important;
  -webkit-user-select: text;
  user-select: text;
}

*, *::before, *::after {
  box-sizing: border-box !important;
}

/* Body text: Quicksand; monospace stacks for code only (matches TipTap reader). */
body, p, li, td, th, blockquote, figcaption, h1, h2, h3, h4, h5, h6, span, div, em, strong, small, label, dt, dd {
  font-family: var(--yaba-font-family, "Quicksand", -apple-system, BlinkMacSystemFont, sans-serif) !important;
}

a {
  font-family: var(--yaba-font-family, "Quicksand", -apple-system, BlinkMacSystemFont, sans-serif) !important;
  color: var(--yaba-primary, #485d92) !important;
}

code, kbd, samp, tt,
pre code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace !important;
}

pre, code, kbd, samp {
  max-width: 100% !important;
}

pre {
  white-space: pre !important;
  overflow-x: auto !important;
  overflow-y: hidden !important;
  width: 100% !important;
  max-width: 100% !important;
  box-sizing: border-box !important;
  page-break-inside: avoid;
}

/* Prevent wide code/tables from painting over adjacent columns in paginated flow */
table {
  display: block !important;
  width: 100% !important;
  max-width: 100% !important;
  overflow-x: auto !important;
  border-collapse: collapse;
}

img, svg, video {
  max-width: 100% !important;
  height: auto !important;
}
`}const ne="epubjs-hl",O={NONE:{fill:"rgba(255, 214, 10, 0.35)","fill-opacity":"1","mix-blend-mode":"multiply"},YELLOW:{fill:"rgba(255, 214, 10, 0.35)","fill-opacity":"1","mix-blend-mode":"multiply"},BLUE:{fill:"rgba(88, 166, 255, 0.35)","fill-opacity":"1","mix-blend-mode":"multiply"},BROWN:{fill:"rgba(193, 154, 107, 0.35)","fill-opacity":"1","mix-blend-mode":"multiply"},CYAN:{fill:"rgba(0, 188, 212, 0.35)","fill-opacity":"1","mix-blend-mode":"multiply"},GRAY:{fill:"rgba(158, 158, 158, 0.35)","fill-opacity":"1","mix-blend-mode":"multiply"},GREEN:{fill:"rgba(102, 187, 106, 0.35)","fill-opacity":"1","mix-blend-mode":"multiply"},INDIGO:{fill:"rgba(92, 107, 192, 0.35)","fill-opacity":"1","mix-blend-mode":"multiply"},MINT:{fill:"rgba(38, 166, 154, 0.35)","fill-opacity":"1","mix-blend-mode":"multiply"},ORANGE:{fill:"rgba(255, 167, 38, 0.35)","fill-opacity":"1","mix-blend-mode":"multiply"},PINK:{fill:"rgba(236, 64, 122, 0.3)","fill-opacity":"1","mix-blend-mode":"multiply"},PURPLE:{fill:"rgba(171, 71, 188, 0.3)","fill-opacity":"1","mix-blend-mode":"multiply"},RED:{fill:"rgba(239, 83, 80, 0.3)","fill-opacity":"1","mix-blend-mode":"multiply"},TEAL:{fill:"rgba(38, 166, 154, 0.3)","fill-opacity":"1","mix-blend-mode":"multiply"}};function oe(e){const n=(e??"YELLOW").toUpperCase();return O[n]??O.YELLOW}let Q=!1,w="compose",b="auto",y=null,r=null,p=null,A=1,V=1,N=null;const C=new Map;let h=null,v=null,U=!1,x=null,B=null,L=[];const F=new WeakSet,re=450;let d={theme:"system",fontSize:"medium",lineHeight:"normal"},T=null,R=null;function D(){!T||!R||(T.removeEventListener("change",R),R=null,T=null)}function ie(){if(typeof window>"u"||typeof window.matchMedia!="function"||d.theme!=="system"||R)return;T=window.matchMedia("(prefers-color-scheme: dark)");const e=()=>{d.theme==="system"&&(H(w,b,null),_("system"),G(d),z())};T.addEventListener("change",e),R=e}function ae(e){return e==="dark"||e==="light"||e==="sepia"||e==="system"?e:"system"}function E(){const e=d.theme;Z(w,b,e,null),e==="system"&&b==="auto"?ie():D(),_(e),G(d);const n=document.getElementById("epub-root");n&&(n.style.background="var(--yaba-reader-bg, transparent)"),z()}function W(e){const n=document.documentElement,o=e.documentElement,l=getComputedStyle(n);o.setAttribute("data-yaba-reader-theme",d.theme);const u=["--yaba-reader-font-size","--yaba-reader-line-height","--yaba-font-family","--yaba-primary"];for(const s of u){const t=l.getPropertyValue(s).trim();t!==""&&o.style.setProperty(s,t)}if(d.theme==="system"){const s=l.getPropertyValue("--yaba-bg").trim(),t=l.getPropertyValue("--yaba-on-bg").trim();s!==""&&o.style.setProperty("--yaba-reader-bg",s),t!==""&&o.style.setProperty("--yaba-reader-on-bg",t)}else{const s=l.getPropertyValue("--yaba-reader-bg").trim(),t=l.getPropertyValue("--yaba-reader-on-bg").trim();s!==""&&o.style.setProperty("--yaba-reader-bg",s),t!==""&&o.style.setProperty("--yaba-reader-on-bg",t)}}function le(e){const n=e.getContents();return Array.isArray(n)?n:[]}function j(e,n,o){return!Array.isArray(e)||e.length===0?[]:e.map((l,u)=>{const s=(l.href??"").trim(),t=`${o}-${u}-${s||"nohref"}`,i=(l.label??"").trim()||"Untitled",c=s.length>0?JSON.stringify({href:s}):null,a=j(l.subitems??[],n+1,t);return{id:t,title:i,level:n,children:a,extrasJson:c}})}async function se(e){try{M();const o=(await e.loaded.navigation)?.toc;if(!o||o.length===0){k({items:[]});return}const l=j(o,1,"epub");k({items:l})}catch{k({items:[]})}}function z(){const e=r;if(e)for(const n of le(e))try{W(n.document)}catch{}}function $(){const e=r;if(e)for(const n of C.values())try{e.annotations.remove(n,"highlight")}catch{}C.clear()}function q(){v&&(clearTimeout(v),v=null)}function Y(){x&&(clearTimeout(x),x=null)}function ce(e){const n=typeof performance<"u"?performance.now():Date.now(),o=B;return o&&o.id===e&&n-o.atMs<re?!1:(B={id:e,atMs:n},!0)}function fe(){q(),v=setTimeout(()=>{v=null;const e=N,n=r;!n||!e||n.display(e)},220)}function de(){if(U||typeof window>"u")return;U=!0;const e=()=>fe();window.addEventListener("resize",e,{passive:!0}),window.visualViewport?.addEventListener("resize",e,{passive:!0})}function ue(e){const n=e.document;if(F.has(n))return;F.add(n);let o=null;const l=()=>{o&&clearTimeout(o),o=setTimeout(()=>{o=null;const u=e.window.getSelection();(!u||u.rangeCount===0||u.isCollapsed||!u.toString().trim())&&(p=null)},500)};n.addEventListener("selectionchange",l,{passive:!0})}function me(e,n){w=e,b=n,H(w,b,null),d={theme:"system",fontSize:"medium",lineHeight:"normal"},E(),de();const o=window;function l(t){const i=r;if(i){$();for(const c of t)if(c.cfiRange){C.set(c.id,c.cfiRange);try{i.annotations.add("highlight",c.cfiRange,{id:c.id},a=>{a.preventDefault(),a.stopPropagation(),ce(c.id)&&o.YabaEpubBridge?.onAnnotationTap?.(c.id)},ne,oe(c.colorRole))}catch{}}}}function u(t=100){Y(),x=setTimeout(()=>{x=null,l(L)},t)}function s(t){let i=[];try{i=t.trim().length>0?JSON.parse(t):[]}catch{i=[]}L=i,l(i)}o.YabaEpubBridge={isReady:()=>Q,setEpubUrl(t){return t?((async()=>{try{if(M(),$(),Y(),L=[],p=null,q(),N=null,B=null,r)try{r.destroy()}catch{}if(r=null,y)try{y.destroy()}catch{}y=null;const i=document.getElementById("epub-root");if(!i){h=null,P("error");return}i.innerHTML="",y=X(t),await y.ready;const c=await y.loaded.spine;if(V=Math.max(1,c.length),await se(y),r=y.renderTo(i,{width:"100%",height:"100%",flow:"paginated",allowScriptedContent:!1}),r.hooks.content.register(a=>{const m=a.document;W(m),ue(a);const f=m.createElement("style");f.id="yaba-epub-content-style",f.textContent=te(),m.head.appendChild(f)}),r.on("relocated",a=>{p=null;const m=a.start.cfi;typeof m=="string"&&m.trim().length>0&&(N=m);const f=a.start.index;typeof f=="number"&&Number.isFinite(f)&&(A=f+1),u(16),queueMicrotask(()=>I())}),r.on("selected",(a,m)=>{const f=m.window.getSelection()?.toString()??"";a&&f.trim().length>0&&(p={cfiRange:a,selectedText:f.trim()}),queueMicrotask(()=>I())}),r.on("rendered",()=>{u(16)}),await r.display(),A=1,z(),P("loaded"),h!==null){const a=h;h=null,s(a)}}catch(i){console.error("EPUB load failed",i),h=null,M(),k({items:[]}),P("error")}})(),!0):!1},getSelectionSnapshot(){return p?{cfiRange:p.cfiRange,selectedText:p.selectedText,prefixText:"",suffixText:""}:null},getCanCreateAnnotation(){return!!(p&&p.selectedText.trim().length>0)},setAnnotations(t){if(!r){h=t;return}h=null,s(t)},scrollToAnnotation(t){const i=C.get(t);i&&r&&r.display(i)},getCurrentPageNumber(){return A},getPageCount(){return V},nextPage(){return r?(r.next(),!0):!1},prevPage(){return r?(r.prev(),!0):!1},setPlatform(t){w=t,E()},setAppearance(t){b=t,E()},setReaderPreferences(t){d={theme:ae(t.theme??d.theme),fontSize:t.fontSize??d.fontSize,lineHeight:t.lineHeight??d.lineHeight},E()},navigateToTocItem(t,i){const c=r;if(c)try{const a=i?.trim();if(!a)return;const f=JSON.parse(a).href;if(!f||typeof f!="string")return;c.display(f)}catch{}}},Q=!0,J({type:"bridgeReady",feature:"epub"})}const S=K();H(S.platform,S.appearance,S.cursorColor);me(S.platform,S.appearance);
