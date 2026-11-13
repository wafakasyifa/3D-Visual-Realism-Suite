# 3D Visual Realism Suite — Praktikum Grafika Komputer Minggu 11
Java 17+ (diuji target JDK 21) · LWJGL 3.3.4 · JOML 1.10.8 · OpenGL 3.3 core / GLSL 330.


## Menjalankan
Prasyarat: JDK 17+ dan Maven, GPU/driver mendukung OpenGL 3.3.

    ./run.sh        # Linux / macOS (otomatis -XstartOnFirstThread di macOS)
    run.bat         # Windows

Atau di IntelliJ / VS Code: buka sebagai **Maven project**, jalankan `Main`
(macOS: tambahkan VM option `-XstartOnFirstThread`).
Native LWJGL dipilih otomatis oleh profil Maven sesuai OS.

## Kontrol
| Tombol | Fungsi (indikator) |
|---|---|
| **B** | Phong <-> Blinn-Phong (1) |
| **F** | Flat (per-face normal) <-> Smooth (vertex normal averaging) (2) |
| **Z** | Z-Buffer Depth Test on/off (3) |
| **C** | Back-Face Culling on/off (3) |
| **T** | Tekstur on/off (4) |
| **N** | Nearest <-> Bilinear + Mipmap (4) |
| **S** | Shadow Mapping on/off (5) |
| **[ / ]** | Shadow bias dibagi 2 / dikali 2 (5) |
| 1 2 3 4 | Sphere / Torus / Cube / House |
| W | Wireframe |
| SPACE / L | Putar objek / orbit cahaya |
| Mouse drag, panah, scroll, R | Orbit, zoom, reset kamera |
| **P** | Screenshot otomatis ke folder `screenshots/` (nama file memuat status fitur) |
| H / ESC | Bantuan / keluar |


## Struktur
      Main, VisualRealismApp   -> window, input, render loop 2-pass
      ShaderProgram            -> compile/link GLSL + uniform
      MeshBuilder, MeshFactory -> mesh + normal flat/smooth + UV (Sphere, Torus, Cube, House, Floor)
      Mesh, MeshData           -> VAO flat & smooth
      HSRManager               -> Z-Buffer & Back-Face Culling
      Texture2D                -> STBImage + mipmap/filter
      ShadowMapFBO             -> Depth FBO 2048x2048
      OrbitCamera
