 (cd "$(git rev-parse --show-toplevel)" && git apply --3way <<'EOF' 
diff --git a/script.js b/script.js
new file mode 100644
index 0000000000000000000000000000000000000000..a4a695678b218f85e0f17cdc1f8fa68231f9ed1a
--- /dev/null
+++ b/script.js
@@ -0,0 +1,25 @@
+const menuToggle = document.querySelector('.menu-toggle');
+const navLinks = document.querySelector('.nav-links');
+const contactForm = document.getElementById('contactForm');
+const statusText = document.querySelector('.form-status');
+
+menuToggle.addEventListener('click', () => {
+  const expanded = menuToggle.getAttribute('aria-expanded') === 'true';
+  menuToggle.setAttribute('aria-expanded', String(!expanded));
+  navLinks.classList.toggle('open');
+});
+
+document.querySelectorAll('.nav-links a').forEach((link) => {
+  link.addEventListener('click', () => {
+    navLinks.classList.remove('open');
+    menuToggle.setAttribute('aria-expanded', 'false');
+  });
+});
+
+contactForm.addEventListener('submit', (event) => {
+  event.preventDefault();
+  const formData = new FormData(contactForm);
+  const name = formData.get('name')?.toString().trim() || 'gamer';
+  statusText.textContent = `Thanks, ${name}! Your message is queued. We'll get back to you soon.`;
+  contactForm.reset();
+});
 
EOF
)
