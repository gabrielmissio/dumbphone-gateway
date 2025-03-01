const app = require('./src/app');

const port = process.env.PORT || 3000;

app.listen(port, () => {
    console.log(`Server is running on http://localhost:${port}`);
    require('dns').lookup(require('os').hostname(), function (err, add, fam) {
        console.log('Server is running on http://' + add + ':' + port);
    });
});
