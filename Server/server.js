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

app.listen(port,()=>{
    console.log("Listening on port: "+port);
});

