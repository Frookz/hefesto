document.addEventListener("DOMContentLoaded", function () {
    var dropZone = document.getElementById('dragDropArea');
    var statusMessage = document.getElementById('statusMessage');

    dropZone.addEventListener('dragover', function (event) {
        event.preventDefault();
        event.dataTransfer.dropEffect = 'copy';
    });

    dropZone.addEventListener('drop', function (event) {
        event.preventDefault();
        var files = event.dataTransfer.files;
        for (var i = 0; i < files.length; i++) {
            var file = files[i];
            if (file.name.endsWith('.sql')) {
                uploadFile(file);
            }
        }
    });

    function uploadFile(file) {
        var formData = new FormData();
        formData.append('file', file);
        // Añadir base package al formData
        var basePackage = document.getElementById('basePackage').value;


        formData.append('basePackage', basePackage);

        var appName = document.getElementById("appName").value;

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