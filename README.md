# ✈️ FPL Project - Lentosuunnitelma sovellus / Flight Planning Application

## 🇫🇮 Suomeksi

Tavoitteena on tehdä lentosuunnitelman tekemisessä auttava järjestelmä. Järjestelmään syötetään muun muassa lähtökenttä, kohde, koneen tiedot, ohjaajan kokemus, lupakirjat, lennon ajankohta sekä muut mahdolliset lisätiedot.

Syötteen perusteella järjestelmä laatii tekoälyn avustamana lentosuunnitelman ja ehdottaa sitä käyttäjälle. Järjestelmän ideana on auttaa ohjaajaa suunnittelemaan lento turvallisesti ja havainnollistaa kartalla asioita, joita ohjaaja ei välttämättä itse muistaisi tai tajuaisi ajatella (esim. sääolosuhteet, reitille osuvat ilmatilat ja NOTAMit). Järjestelmää voisi tulevaisuudessa laajentaa niin, että se toimisi lennon aikana esimerkiksi laskien jatkuvasti lähimpiä hätälaskukenttiä.

**[➡️ Testaa täällä / Test the application](fpl-project-eliashaverinen-gjhme4chghhhgnag.swedencentral-01.azurewebsites.net)**

---

## 🇬🇧 In English

This is an AI-assisted flight planning application designed to help pilots create safe and well-optimized flight plans. By taking into account the pilot's experience, aircraft details, weather, and airspace restrictions, the system generates a comprehensive briefing and visualizes the route on a map.

### 🛠️ How it's built (Tech Stack)

I built this project to practice full-stack Java development, API integrations, and handling geospatial data.

* **Backend:** Java & Spring Boot. The backend handles the business logic, API calls, and processes large amounts of aviation data (airspaces, airports) efficiently in the memory.
* **Frontend & Map:** HTML, CSS, and Leaflet.js. The backend generates an HTML page that uses Leaflet to draw the route and restricted areas dynamically using GeoJSON.
* **Data Handling:** Jackson is used heavily to parse and format JSON/GeoJSON data between the backend and the map.
* **Cloud / Hosting:** The application is automatically deployed to Microsoft Azure Web Apps using GitHub Actions (CI/CD).

### ⚙️ Current Features (MVP)
- [x] Loads and filters European airspaces, airports, and navaids.
- [x] Fetches live weather data based on the flight's route.
- [x] Generates a dynamic map drawing the route and relevant points of interest.
- [x] Automated deployment to Azure.
- [ ] Integrating OpenAI API to analyze route safety based on pilot experience.
- [ ] Integrating live NOTAMs via Autorouter API.

## 💻 Local Setup (For developers)

1. Clone the repository:
   ```bash
   git clone [https://github.com/EliasHave/FPL_Project.git](https://github.com/EliasHave/FPL_Project.git)

2. Build with Maven:

    ```Bash
    mvn clean install
   ```
   
3. Run the Spring Boot application:

    ```Bash
    mvn spring-boot:run
    ```

4. Open your browser and go to http://localhost:8080.