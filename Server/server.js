const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const routes = require("./routes");

const app = express();

var port = 3000;
var version = "v1";

app.use(cors());
app.use(bodyParser.urlencoded({extended:false}));
app.use(bodyParser.json());

app.use('/api/'+version,routes);

const fs = require('fs');

app.use('/',express.static('public'));

app.listen(port,()=>{
    console.log("Listening on port: "+port);
});

app.get('/homepage', (req, res) => {
    res.sendFile(__dirname+'/public' + '/homePage.html');
});

app.get('/teams', (req, res) => {
    res.sendFile(__dirname+'/public' + '/createOrViewTeam.html');
});

app.get('/examiners', (req, res) => {
    res.sendFile(__dirname+'/public' + '/createOrViewExaminers.html');
});

app.get('/', (req, res) => {
    res.sendFile(__dirname+'/public' + '/index.html');
});