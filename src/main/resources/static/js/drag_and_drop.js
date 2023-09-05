document.addEventListener("DOMContentLoaded", function () {
    let dropZone = document.getElementById('dragDropArea');
    let statusMessage = document.getElementById('statusMessage');

    dropZone.addEventListener('dragover', function (event) {
        event.preventDefault();
        event.dataTransfer.dropEffect = 'copy';
    });

    dropZone.addEventListener('drop', function (event) {
        event.preventDefault();
        let files = event.dataTransfer.files;
        for (let i = 0; i < files.length; i++) {
            let file = files[i];
            if (file.name.endsWith('.sql')) {
                uploadFile(file);
            }
        }
    });

    function uploadFile(file) {
        let formData = new FormData();
        formData.append('file', file);

        let basePackage = document.getElementById('basePackage').value;
        formData.append('basePackage', basePackage);

        let appName = document.getElementById("appName").value;
        formData.append('appName', appName);

        fetch('/upload', {
            method: 'POST',
            body: formData
        }).then(response => response.json())
            .then(data => {
                statusMessage.innerHTML = "Clases generadas";
                statusMessage.style.display = "block";
            }).catch(error => {
            console.error("Error al cargar el archivo:", error);
        });
    }
});