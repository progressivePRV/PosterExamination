const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const routes = require("./routes");
const cookieParser = require('cookie-parser');

const app = express();

var port = 3000;
var version = "v1";
const tokenSecret = "wFq9+ssDbT#e2H9^";

var verifyCookie = function(req,res,next){
    var cookieValue = req.cookie;
    if(!cookieValue){
        //closeConnection();
        return res.status(400).json({"error":"Cookie not provided user authorization"});
    }

    var authData = req.cookie;

    if(authData){
        token = authData;
        try {
            decoded = jwt.verify(token, tokenSecret);
            if(!decoded || !decoded.role){
                return res.status(400).json({"error":"user role not mentioned in token for user authorization"});
            }
            if(decoded.role!=='admin'){
                return res.status(400).json({"error":"unauthorized user"});
            }
            next();
          } catch(err) {
            //closeConnection();
            return res.status(400).json({"error":err});
          }
    }
    else {
        //closeConnection();
        return res.status(400).json({"error":"Appropriate authentication information needs to be provided"})
    }

};

app.use(cors());
app.use(bodyParser.urlencoded({extended:false}));
app.use(bodyParser.json());
app.use(cookieParser());

app.use('/api/'+version,routes);

const fs = require('fs');
const { request } = require('express');

app.use('/',express.static('public'));

app.listen(port,()=>{
    console.log("Listening on port: "+port);
});

app.get('/homepage', verifyCookie, (req, res) => {
    res.sendFile(__dirname+'/public' + '/homePage.html');
});

app.get('/teams', verifyCookie, (req, res) => {
    res.sendFile(__dirname+'/public' + '/createOrViewTeam.html');
});

app.get('/examiners', verifyCookie, (req, res) => {
    res.sendFile(__dirname+'/public' + '/createOrViewExaminers.html');
});

app.get('/', (req, res) => {
    console.log('hello');
    res.sendFile(__dirname+'/public' + '/index.html');
});