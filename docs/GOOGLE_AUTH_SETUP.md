# Google OAuth en Android

La app inicia el flujo en el navegador del sistema con PKCE. El código de autorización vuelve a `com.myfarmio.app://auth/callback`; el verificador se conserva cifrado con `SessionManager` y se intercambia con Supabase Auth. No se incorpora ningún secreto OAuth al APK.

## Configuración externa

1. En Google Cloud, configurá la pantalla de consentimiento y creá un cliente OAuth de tipo **Web application** para el proveedor de Supabase. Habilitá los alcances `openid`, `email` y `profile`.
2. En Google Cloud, registrá como URI de redirección autorizada la URL de callback que muestra el panel de Supabase para el proveedor Google (`https://<project-ref>.supabase.co/auth/v1/callback`).
3. En Supabase → Authentication → Sign In / Providers → Google, habilitá Google y cargá allí el Client ID y Client Secret de Google. El secret queda únicamente en Supabase.
4. En Supabase → Authentication → URL Configuration, agregá `com.myfarmio.app://auth/callback` a la lista de Redirect URLs permitidas.
5. Confirmá que el proyecto Android use la URL de Supabase y la clave anon/publishable que ya usa la web. No pongas `service_role` en mobile.

## Prueba

Instalá la app, tocá **Continuar con Google**, elegí una cuenta autorizada y aceptá. Android debe volver a MyFarmio; Supabase debe devolver una sesión y la app debe cargar la membresía activa antes de abrir el inicio. Probá también cancelar y un usuario sin organización: la app debe volver al login o mostrar el error correspondiente, sin crear organizaciones ni permisos.

El flujo no se puede validar de extremo a extremo hasta completar las credenciales del proveedor en Google Cloud y Supabase. La app verifica el callback y usa el intercambio PKCE, pero la autorización remota depende de esa configuración.
