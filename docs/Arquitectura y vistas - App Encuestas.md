# App Encuestas — guía sencilla de pantallas

Esta nota explica qué hace cada pantalla, qué componentes usa y qué tecnología hay detrás. Está basada en el código actual del proyecto.

## Tecnologías principales

- **Android nativo:** Java para la lógica y XML para las pantallas.
- **Material Components:** botones, campos, tarjetas, barras superiores y navegación.
- **Firebase Authentication:** inicio de sesión con correo y contraseña o cuenta Google.
- **Cloud Firestore:** guarda usuarios e historial de accesos.
- **Google Forms API:** crea y edita formularios y consulta respuestas.
- **SharedPreferences:** guarda datos solo en el teléfono, como borradores y preferencias.
- **MPAndroidChart:** dibuja los gráficos.

Firebase y Google Forms cumplen funciones distintas: iniciar sesión en la app no autoriza automáticamente el acceso a los formularios. Para usar Google Forms, la app solicita permiso aparte.

## Pantallas

### Inicio de sesión — `LoginActivity`

- **Componentes:** `TextInputEditText` `etLoginEmail` y `etLoginPassword`; `MaterialButton` `btnLogin` y `btnGoogle`; `TextView` `tvForgot`; `ImageView` `ivLoginLogo`.
- **Cómo funciona:** valida la cuenta con Firebase. El botón Google inicia sesión con Google y conecta esa identidad con Firebase.
- **Por qué:** permite entrar con correo o con una cuenta Google.

> El acceso con un nombre de usuario simple está implementado como flujo local/de demostración: no comprueba una contraseña en Firebase.

### Panel principal — `MainActivity`

- **Componentes:** `MaterialToolbar` `toolbar`; `TabLayout` `dashboardTabs`; `ViewPager2` `dashboardPager`; `RecyclerView` `dashboardRecycler`; `BarChart` `quickChart`; `BottomNavigationView` `bottomNavigation`.
- **Cómo funciona:** muestra pestañas de encuestas e inicios recientes. El historial puede cargarse desde Firestore.
- **Por qué:** reúne accesos y resúmenes en una pantalla.

> Las encuestas del panel, el gráfico y el contador de encuestas son datos de ejemplo/fijos en el código.

### Encuestas — `SurveyManagementActivity`

- **Componentes:** `MaterialToolbar` `surveyToolbar`; botones `showPublishedSurveys`, `showSurveyDrafts` y `btnCreateGoogleForm`; `RecyclerView` `surveyRecycler`.
- **Cómo funciona:** alterna entre formularios publicados y borradores. Los borradores se guardan en el teléfono; los formularios creados desde la app guardan su ficha localmente.
- **Por qué:** separa encuestas listas de las que todavía se están preparando.

### Crear encuesta — `CreateSurveyActivity`

- **Componentes:** `MaterialToolbar` `createSurveyToolbar`; campos `newSurveyTitle` y `newSurveyDescription`; `Spinner` `newSurveyCategory`; `LinearLayout` `surveyQuestionsContainer`; botones `addSurveyQuestion`, `submitNewSurvey` y `saveSurveyDraft`.
- **Cómo funciona:** permite agregar preguntas, guardar un borrador local o pedir permiso y publicar en Google Forms.
- **Por qué:** se puede preparar la encuesta antes de publicarla.

### Pregunta de encuesta — `item_create_survey_question.xml`

- **Componentes:** `TextInputEditText` `surveyQuestionInput`; `Spinner` `surveyQuestionAnswerType`; botón `removeSurveyQuestion`.
- **Cómo funciona:** esta fila se repite por cada pregunta, tanto al crear como al editar.
- **Por qué:** permite agregar y quitar preguntas sin duplicar el diseño.

### Editar encuesta — `EditSurveyActivity`

- **Componentes:** campos `editSurveyTitle` y `editSurveyDescription`; selectores `editSurveyCategory` y `editSurveyStatus`; lista `editQuestionsContainer`; botón `saveSurveyChanges`.
- **Cómo funciona:** carga el formulario de Google, pide autorización y envía los cambios a Google Forms.
- **Por qué:** permite actualizar preguntas y estado desde la app.

### Histórico — `SurveyHistoryActivity`

- **Componentes:** `MaterialToolbar` `historyToolbar`; `RecyclerView` `historyCategories`.
- **Cómo funciona:** muestra categorías y cuántas encuestas locales hay en cada una. Al tocar una categoría, abre su lista.
- **Por qué:** ayuda a encontrar encuestas por tema.

### Encuestas por categoría — `CategorySurveysActivity`

- **Componentes:** `MaterialToolbar` `categorySurveysToolbar`; `Spinner` `categoryStatusFilter`; `RecyclerView` `categorySurveysRecycler`; botón `exportCategoryReport`.
- **Cómo funciona:** filtra por estado y permite exportar el reporte.
- **Por qué:** facilita revisar o exportar una parte del histórico.

### Estadísticas — `StatisticsActivity`

- **Componentes:** selectores `statisticsCategoryFilter`, `statisticsSurveyFilter` y `statisticsQuestionFilter`; gráficos `statisticsChart`, `statisticsPieChart` y `statisticsTrendChart`; botón `exportStatisticsButton`.
- **Cómo funciona:** solicita permiso de Google, consulta respuestas reales de formularios guardados por la app y muestra totales, distribución y tendencia.
- **Por qué:** resume las respuestas con gráficos y filtros.

### Vista previa y exportación — `ExportPreviewActivity`

- **Componentes:** `MaterialToolbar` `exportPreviewToolbar`; `LinearLayout` `exportPreviewContent`; botón `downloadReportButton`.
- **Cómo funciona:** muestra una vista previa y guarda el reporte como PDF o Excel.
- **Por qué:** permite revisar el archivo antes de guardarlo.

### Perfil — `PerfilActivity`

- **Componentes:** `MaterialToolbar` `toolbar`; textos `tvProfileName` y `tvProfileEmail`; campos `etName`, `etEmail`, `etCurrent` y `etNew`; botones `btnSave`, `btnUpdatePass` y `btnLogout`.
- **Cómo funciona:** el nombre y el correo se guardan localmente. La contraseña se actualiza en Firebase si hay una sesión autenticada.
- **Por qué:** reúne los datos del usuario y las acciones de cuenta.

### Configuración — `ConfiguracionActivity`

- **Componentes:** `MaterialToolbar` `settingsToolbar`; campo `etSystemName`; imagen `systemLogoPreview`; botones `btnChangeLogo`, `btnNewPalette` y `btnSaveSettings`; lista `customPaletteContainer`.
- **Cómo funciona:** permite cambiar nombre, logo y colores. La configuración se guarda en el teléfono.
- **Por qué:** personaliza la apariencia de la app.

### Usuarios — `UsersActivity`

- **Componentes:** `MaterialToolbar` `usersToolbar`; tabla `usersTable`; texto `usersEmpty`.
- **Cómo funciona:** muestra usuarios e historial cargados localmente o desde Firestore y permite cambiar su estado.
- **Por qué:** facilita revisar cuentas registradas.

> `UsersActivity` existe, pero falta declararla en `AndroidManifest.xml`; por eso, el acceso desde el menú puede fallar.

## Qué datos vienen de cada lugar

- **Firebase Auth:** identidad de la sesión.
- **Firestore:** usuarios e historial de inicios de sesión.
- **Google Forms:** formularios y respuestas consultados con permiso OAuth.
- **Teléfono:** borradores, preferencias y fichas locales de formularios creados desde la app.
- **Datos de ejemplo:** encuestas del histórico y varios números/gráficos del panel principal.

## Archivos para ubicar cada parte

- `app/src/main/AndroidManifest.xml`: pantallas registradas en Android.
- `app/src/main/res/layout/`: diseños XML.
- `app/src/main/java/com/example/app_encuestas/`: código Java.
- `LoginHistoryStore.java`: usuarios e historial local/Firestore.
- `GoogleFormsApi.java`: conexión con Google Forms.
- `MockSurveyData.java`: ejemplos y registros locales de encuestas.
- `firestore.rules`: reglas de acceso de Firestore.
