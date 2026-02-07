fetch('/api/players')
    .then(response => response.json())
    .then(data => {
        const list = document.getElementById('players-list');
        data.forEach(player => {
            list.innerHTML += `<p>${player.username} - Level ${player.level} ${player.class}</p>`;
        });
    });
