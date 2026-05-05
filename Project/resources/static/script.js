let currentStep = 1;

function goTo(step) {
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    document.getElementById('panel' + step).classList.add('active');

    document.querySelectorAll('.step').forEach((s, i) => {
        s.classList.remove('active', 'done');
        if (i + 1 < step) s.classList.add('done');
        if (i + 1 === step) s.classList.add('active');
    });

    currentStep = step;
}

function updateRoute() {
    const from = document.getElementById('lahtoKentta').value || '—';
    const to = document.getElementById('maaranpaa').value || '—';
    document.getElementById('routeFrom').textContent = from;
    document.getElementById('routeTo').textContent = to;
}

async function suunnitteleLento() {
    // Piilota paneelit, näytä loading
    document.getElementById('panel3').style.display = 'none';
    document.getElementById('loading').classList.add('visible');

    const data = {
        lahtoKentta: document.getElementById('lahtoKentta').value,
        maaranpaa: document.getElementById('maaranpaa').value,
        lahtoAika: document.getElementById('lahtoAika').value,
        rekNro: document.getElementById('rekNro').value,
        koneTyyppi: document.getElementById('koneTyyppi').value,
        kategoria: document.getElementById('kategoria').value,
        cruiseSpeed: parseFloat(document.getElementById('cruiseSpeed').value) || 0,
        climbRate: parseFloat(document.getElementById('climbRate').value) || 0,
        maxAltitude: parseFloat(document.getElementById('maxAltitude').value) || 0,
        kulutus: parseFloat(document.getElementById('kulutus').value) || 0,
        range: parseFloat(document.getElementById('range').value) || 0,
        maxFlightTime: parseFloat(document.getElementById('maxFlightTime').value) || 0,
        tankSize: parseFloat(document.getElementById('tankSize').value) || 0,
        usableFuel: parseFloat(document.getElementById('usableFuel').value) || 0,
        reserve: parseFloat(document.getElementById('reserve').value) || 0,
        emptyWeight: parseFloat(document.getElementById('emptyWeight').value) || 0,
        mtow: parseFloat(document.getElementById('mtow').value) || 0,
        usefulLoad: parseFloat(document.getElementById('usefulLoad').value) || 0,
        payLoad: parseFloat(document.getElementById('payLoad').value) || 0,
        gps: document.getElementById('gps').value,
        vor: document.getElementById('vor').value,
        radio: document.getElementById('radio').value,
        transponder: document.getElementById('transponder').value,
        nimi: document.getElementById('nimi').value,
        puhnro: document.getElementById('puhnro').value,
        sposti: document.getElementById('sposti').value,
        syntymapaiva: document.getElementById('syntymapaiva').value,
        kokemus: parseInt(document.getElementById('kokemus').value) || 0,
        lupakirja: document.getElementById('lupakirja').value
    };

    try {
        const res = await fetch('/api/flight/suunnittele', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        const text = await res.text();
        const url = text.trim();

        // Ei uuteen välilehteen vaan samaan
        window.location.href = url;
    } catch (err) {
        alert('Virhe: ' + err.message);
        document.getElementById('loading').classList.remove('visible');
        document.getElementById('panel3').style.display = 'block';
    }
}

