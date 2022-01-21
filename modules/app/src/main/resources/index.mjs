let patientNotes = {notes: [], docIndex: -1, name: undefined};

function updateNavButtonStatus(prevBtn, nextBtn)
{
    prevBtn.disabled = patientNotes.docIndex <= 0;
    nextBtn.disabled = patientNotes.docIndex === patientNotes.notes.length - 1
}

function renderContent(txt, nType, nDt, docIndex) {
    txt.innerHTML = patientNotes.notes[docIndex].text;
    nType.innerHTML = '<span>' + patientNotes.notes[docIndex].noteType + '</span>';;
    nDt.innerHTML = '<span>' + new Date(patientNotes.notes[docIndex].timestamp) + '</span>';
}

document.addEventListener('DOMContentLoaded', () =>
{
    let pid = document.querySelector('#mrn');
    let dt = document.querySelector('#after');
    let txt = document.querySelector("#note");
    let pName = document.querySelector("#patient");
    let prevBtn = document.querySelector('#prev');
    let nextBtn = document.querySelector('#next');
    let nType = document.querySelector("#noteType");
    let nDt = document.querySelector("#noteDate");
    updateNavButtonStatus(prevBtn, nextBtn);
    prevBtn.addEventListener('click', evt =>
    {
        if(patientNotes.notes.length > 0 && patientNotes.docIndex > 0)
        {
            renderContent(txt, nType, nDt, --patientNotes.docIndex);
        }
        updateNavButtonStatus(prevBtn, nextBtn);
    });
    nextBtn.addEventListener('click', evt =>
    {
        if(patientNotes.notes.length > 0 && patientNotes.docIndex < patientNotes.notes.length)
        {
            renderContent(txt, nType, nDt, ++patientNotes.docIndex);
        }
        updateNavButtonStatus(prevBtn, nextBtn);
    });
    document.querySelector("#search").addEventListener('click', evt =>
    {
        let httpRequest = new XMLHttpRequest();
        httpRequest.open('GET', '/fhir?mrn=' + pid.value + '&' + 'after=' + dt.value.split('T')[0])
        httpRequest.onreadystatechange = () =>
        {
            if (httpRequest.readyState === XMLHttpRequest.DONE && httpRequest.status === 200)
            {
                let noteObj = JSON.parse(httpRequest.responseText);
                pName.innerHTML = patientNotes.name  = 'Patient Name:' + noteObj.names[0];
                if(noteObj.notes.length > 0)
                {
                    patientNotes.notes = noteObj.notes;
                    patientNotes.docIndex = 0;
                    renderContent(txt, nType, nDt, 0);
                    updateNavButtonStatus(prevBtn, nextBtn);
                }
            }
        };
        httpRequest.send();
        patientNotes = {notes: [], docIndex: -1, name: undefined};
        txt.innerHTML = '<span style="margin: 0 auto;font-weight:bold;">Loading... ...</span>';
        updateNavButtonStatus(prevBtn, nextBtn);
    });
});