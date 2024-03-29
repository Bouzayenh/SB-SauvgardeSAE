document.getElementById('operationForm').addEventListener('submit', function(event) {
    event.preventDefault();

    const folderPath = document.getElementById('folderPath').value;
    const operation = document.getElementById('operation').value;
    const useZip = document.getElementById('useZip').checked;
    const extensions = document.getElementById('extensions').value.split(',');

    const payload = {
        folderPath: folderPath,
        operation: operation,
        useZip: useZip,
        extensions: extensions
    };

    // TODO: remplacer par l'url du vrai serveur a la fin
    const url = 'http://localhost:8082/performOperation';

    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(response => response.text())
        .then(data => {
            document.getElementById('response').textContent = 'Response: ' + data;
        })
        .catch((error) => {
            console.error('Error:', error);
        });
});
