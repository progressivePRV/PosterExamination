const express = require("express");
const mongo = require('mongodb');
const jwt = require('jsonwebtoken');
const { requestBody, validationResult, body, header, param, query } = require('express-validator');
const bcrypt = require('bcryptjs');
const User = require("./User");
const { response, request } = require("express");
const fs = require('fs');
const { userInfo } = require("os");

const MongoClient = mongo.MongoClient;
const uri = "mongodb+srv://rojatkaraditi:AprApr_2606@test.z8ya6.mongodb.net/ScopeDB?retryWrites=true&w=majority";
var client;
var collection;
var teamsCollection;
const tokenSecret = "wFq9+ssDbT#e2H9^";
var decoded={};
var token;
var loggedInUser;


var connectToDb = function(req,res,next){
    client = new MongoClient(uri, { useNewUrlParser: true, useUnifiedTopology: true});
    client.connect(err => {
      if(err){
          closeConnection();
          return res.status(400).json({"error":"Could not connect to database: "+err});
      }
      collection = client.db("ScopeDB").collection("users");
      teamsCollection = client.db("ScopeDB").collection("teams");
      console.log("connected to database");
    next();
    });
};

var closeConnection = function(){
    client.close();
}

var verifyToken = function(req,res,next){
    var headerValue = req.header("Authorization");
    if(!headerValue){
        //closeConnection();
        return res.status(400).json({"error":"Authorization header needs to be provided for using API"});
    }

    var authData = headerValue.split(' ');

    if(authData && authData.length==2 && authData[0]==='Bearer'){
        token = authData[1];
        try {
            decoded = jwt.verify(token, tokenSecret);
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

var isAuthorisedExaminer = function(request,response,next){
    if(decoded){
        if(decoded.role){
            if(decoded.role==='examiner'){
                var query = {"_id":new mongo.ObjectID(decoded._id)};
                        collection.find(query).toArray((err,res)=>{
                            if(err){
                                closeConnection();
                                return response.status(400).json({"error":err});
                            }
                            if(res.length<=0){
                                closeConnection();
                                return response.status(400).json({"error":"could not find user with id: "+decoded._id});
                            }
                            loggedInUser  = new User(res[0]).getUser();
                            if(loggedInUser && loggedInUser.role && loggedInUser.role!=='examiner'){
                                closeConnection();
                                return response.status(400).json({"error":"Unauthorized user"});
                            }
                            next();
                        });
            }
            else{
                closeConnection();
                return response.status(400).json({"error":"Unauthorized user"});
            }
        }
        else{
            closeConnection();
            return response.status(400).json({"error":"No role assigned to user"});
        }
    }else{
        closeConnection();
        return response.status(400).json({"error":"Appropriate authentication information needs to be provided"});
    }
};

var isAuthorisedAdmin = function(request,response,next){
    if(decoded){
        if(decoded.role){
            if(decoded.role==='admin'){
                var query = {"_id":new mongo.ObjectID(decoded._id)};
                        collection.find(query).toArray((err,res)=>{
                            if(err){
                                closeConnection();
                                return response.status(400).json({"error":err});
                            }
                            if(res.length<=0){
                                closeConnection();
                                return response.status(400).json({"error":"could not find user with id: "+decoded._id});
                            }
                            loggedInUser  = new User(res[0]).getUser();
                            if(loggedInUser && loggedInUser.role && loggedInUser.role!=='admin'){
                                closeConnection();
                                return response.status(400).json({"error":"Unauthorized user"});
                            }
                            next();
                        });
            }
            else{
                closeConnection();
                return response.status(400).json({"error":"Unauthorized user"});
            }
        }
        else{
            closeConnection();
            return response.status(400).json({"error":"No role assigned to user"});
        }
    }else{
        closeConnection();
        return response.status(400).json({"error":"Appropriate authentication information needs to be provided"});
    }
};

const route = express.Router();
route.use("/admin",verifyToken);
route.use("/examiner",verifyToken);
route.use(connectToDb);
route.use("/admin",isAuthorisedAdmin);
route.use("/examiner",isAuthorisedExaminer);


route.post("/admin/examiners",[
    body("firstName","firstName cannot be empty").notEmpty().trim().escape(),
    body("firstName","firstName can have only alphabets").isAlpha().trim().escape(),
    body("lastName","lastName cannot be empty").notEmpty().trim().escape(),
    body("lastName","lastName can have only alphabets").isAlpha().trim().escape(),
    body("email","email cannot be empty").notEmpty().trim().escape(),
    body("email","invalid email format").isEmail(),
    body("password","password cannot be empty").notEmpty().trim(),
    body("password","password should have atleast 6 and at max 20 characters").isLength({min:6,max:20})
],(request,response)=>{
    const err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }
    try{
        let pwd = request.body.password;
        var hash = bcrypt.hashSync(pwd,10);
        var newUser = new User(request.body);
        newUser.password=hash;
        newUser.role = "examiner";

        collection.insertOne(newUser,(err,res)=>{
            var result={};
            if(err){
                closeConnection();
                return response.status(400).json({"error":err});
            }
            else{
                if(res.ops.length>0){
                    result = res.ops[0].getUser();
                    closeConnection();
                    return response.status(200).json(result);
                }
                else{
                    closeConnection();
                    return response.status(400).json({"error":"user could not be created"});
                }
                
            }
        });
    }
    catch(error){
        closeConnection();
        return response.status(400).json({"error":error});
    }
}); 


route.get("/users/login",[
    header("Authorization","Authorization header required to login").notEmpty().trim()
],(request,response)=>{

    const err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }
    
    try{
        var data = request.header('Authorization');
        //console.log(data);
        var authData = data.split(' ');

        if(authData && authData.length==2 && (authData[0]==='Basic' || authData[0]==='Bearer')){

            if(authData[0]==='Basic'){
                let buff = new Buffer(authData[1], 'base64');
                let loginInfo = buff.toString('ascii').split(":");
                var result ={};
    
                if(loginInfo!=undefined && loginInfo!=null && loginInfo.length==2){
                    var query = {"email":loginInfo[0]};
                    collection.find(query).toArray((err,res)=>{
                        var responseCode = 400;
                        if(err){
                            result = {"error":err};
                        }
                        else if(res.length<=0){
                            result={"error":"no such user present"};
                        }
                        else{
                            var user = new User(res[0]);
                            if(bcrypt.compareSync(loginInfo[1],user.password)){
                                result=user.getUser();
                                user=user.getUser();
                                //user.exp = Math.floor(Date.now() / 1000) + (60 * 60);
                                //for testing only. uncomment line 234 for final implementation
                                user.exp = Math.floor(Date.now() / 1000) + (60 * 5);
                                var token = jwt.sign(user, tokenSecret);
                                result.token=token;
                                responseCode=200;
                            }
                            else{
                                result={"error":"Username or password is incorrect"};
                            }
                        }
                        closeConnection();
                        return response.status(responseCode).json(result);
    
                    });
                }
                else{
                    closeConnection();
                    return response.status(400).json({"error":"credentials not provided for login"});
                }
            }
            else if(authData[0]==='Bearer'){
                var examinerToken = authData[1];
                try {
                    var decodedToken = jwt.verify(examinerToken, tokenSecret);
                    // console.log(decodedToken)
                    if(decodedToken && decodedToken.role && decodedToken.role==='examiner'){
                        var query = {"_id":new mongo.ObjectID(decodedToken._id)};
                        //var query = {"_id":decodedToken._id};
                        collection.find(query).toArray((err,res)=>{
                            if(err){
                                closeConnection();
                                return response.status(400).json({"error":err});
                            }
                            if(res.length<=0){
                                closeConnection();
                                return response.status(400).json({"error":"could not find user with id: "+decodedToken._id});
                            }
                            var usr = new User(res[0]).getUser();
                            if(usr && usr.role && usr.role!=='examiner'){
                                closeConnection();
                                return response.status(400).json({"error":"only an examiner can use this login method"});
                            }

                            usr.token=examinerToken;
                            closeConnection();
                            return response.status(200).json(usr);
                        });
                    }
                    else{
                        closeConnection();
                        return response.status(400).json({"error":"only an examiner can use this login method"});
                    }
                } catch(err) {
                    closeConnection();
                    //console.log(err);
                    return res.status(400).json({"error":err});
                }
            }
        }
        else{
            closeConnection();
            return response.status(400).json({"error":"Desired authentication type and value required for login"})
        }
    }
    catch(error){
        closeConnection();
        return response.status(400).json({"error":error.toString()});
    }

});


route.get("/examiner/questions",(request,response)=>{
    try{
        var rawdata = fs.readFileSync('questions.json');
        if(!rawdata){
            closeConnection();
            return response.status(400).json({"error":"No questions file found"});
        }
        var questions = JSON.parse(rawdata);
        closeConnection();
        return response.status(200).json(questions);
    }
    catch(error){
        closeConnection();
        return response.status(400).json({"error":error.toString()});
    }
});

route.get("/examiner/questions/:id",[
    param('id','id should be an integer value').isInt()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }

    try{
        var rawdata = fs.readFileSync('questions.json');
        if(!rawdata){
            closeConnection();
            return response.status(400).json({"error":"No questions file found"});
        }
        var questions = JSON.parse(rawdata);
        if(questions && questions.results && questions.results.length>0){
            var question = questions.results.find(it=>it.id==request.params.id);
            if(question){
                closeConnection();
                return response.status(200).json(question);
            }
            else{
                closeConnection();
                return response.status(400).json({"error":"No question found with id "+request.params.id});
            }
        }
        else{
            closeConnection();
            return response.status(400).json({"error":"No questions present"});
        }
        
    }
    catch(error){
        closeConnection();
        return response.status(400).json({"error":error.toString()});
    }
});

route.post('/admin/teams',[
    body('name','team name is required to create a new team').notEmpty().trim().escape(),
    body('name','team name cannot be a numeric value').not().isNumeric(),
    body('members','team members required to create team').notEmpty().trim().escape(),
    body('members','team members cannot be a numeric value').not().isNumeric()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }

    try{
        var team={};
        var members = request.body.members.split(",");
        if(members && members.length>0 && members.length<=4){
            for(var i=0;i<members.length;i++){
                members[i]=members[i].trim();
            }
            team.name = request.body.name.trim();
            team.members = members;
            team.averageScore = 0;
            team.examiners = [];

            teamsCollection.insertOne(team,(err,res)=>{
                var result={};
                if(err){
                    closeConnection();
                    return response.status(400).json({"error":err});
                }
                else{
                    if(res.ops.length>0){
                        result = res.ops[0];
                        closeConnection();
                        return response.status(200).json(result);
                    }
                    else{
                        closeConnection();
                        return response.status(400).json({"error":"team could not be created"});
                    }
                    
                }
            });

        }
        else{
            closeConnection();
            return response.status(400).json({"error":"a team can have minimum 1 and maximum 4 members"});
        }
    }
    catch(error){
        closeConnection();
        return response.status(400).json({"error":error.toString()});
    }
});

route.get('/examiner/teams',[
    header('teamToken','teamToken should be of correct format').optional().isJWT()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }
   
    var query={};

    if(request.header('teamToken')){
        try{
            var decodedToken = jwt.verify(request.header('teamToken'), tokenSecret);
            query._id=mongo.ObjectID(decodedToken.id);
        }catch(error){
            closeConnection();
            return response.status(400).json({"error":error.toString(),"errorOn":"team"});
        }
    }

    teamsCollection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no teams found"});
        }

        if(!request.header('teamToken')){
            var teams = res;
            for(var i=0;i<teams.length;i++){
                delete teams[i].examiners;
            }
            closeConnection();
            return response.status(200).json(teams);
        }
        else{
            var team = res[0];
            delete team.examiners;
            closeConnection();
            return response.status(200).json(team);
        }
    });
});

route.get('/admin/teams/:id',[
    param('id','id is required to search examiner by id').notEmpty().trim().escape(),
    param('id','id should be of correct format').isMongoId()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }

    var rawdata = fs.readFileSync('questions.json');
    if(!rawdata){
        closeConnection();
        return response.status(400).json({"error":"No questions file found"});
    }
    var qs = JSON.parse(rawdata);

    var query = {"_id":mongo.ObjectID(request.params.id)};

    teamsCollection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no team found"});
        }

        var team = res[0];
            var examiners = team.examiners;
            for(var j=0;j<examiners.length;j++){
                var questions = examiners[j].questions;
                for(var k=0;k<questions.length;k++){
                    var question = qs.results.find(q=>q.id==questions[k].id);
                    team.examiners[j].questions[k].question = question.question;
                }
            }
        closeConnection();
        return response.status(200).json(team);
    });
});

route.get('/admin/teams',[
    query('name','name cannot be a numeric value').optional().not().isNumeric()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }

    var query={};
    if(request.query.name){
        var rule = {"$regex": ".*"+request.query.name+".*", "$options": "i"}
        query.name = rule;
    }

    var rawdata = fs.readFileSync('questions.json');
    if(!rawdata){
        closeConnection();
        return response.status(400).json({"error":"No questions file found"});
    }
    var qs = JSON.parse(rawdata);

    teamsCollection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no teams found"});
        }

        var teams = res;
        for(var i=0;i<teams.length;i++){
            var examiners = teams[i].examiners;
            for(var j=0;j<examiners.length;j++){
                var questions = examiners[j].questions;
                for(var k=0;k<questions.length;k++){
                    var question = qs.results.find(q=>q.id==questions[k].id);
                    teams[i].examiners[j].questions[k].question = question.question;
                }
            }
        }
        closeConnection();
        return response.status(200).json(teams);
    });
});

route.get('/admin/examiners',[
    query("firstName","firstName should only have alphabets").optional().isAlpha().trim().escape(),
    query("lastName","lastName should only have alphabets").optional().isAlpha().trim().escape()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }

    var query = {};
    if(request.query.firstName){
        var rule = {"$regex": ".*"+request.query.firstName+".*", "$options": "i"}
        query.firstName=rule;
    }
    if(request.query.lastName){
        var rule = {"$regex": ".*"+request.query.lastName+".*", "$options": "i"}
        query.lastName=rule;
    }
    if(request.query.email){
        var rule = {"$regex": ".*"+request.query.email+".*", "$options": "i"}
        query.email=rule;
    }

    collection.find(query,{ projection: { password: 0 } }).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no examiners found"});
        }

        var users = res;
        var examiners=[];
        for(var i=0;i<users.length;i++){
            if(users[i].role==='examiner'){
                examiners.push(users[i]);
            }
        }

        if(examiners && examiners.length>0){
            closeConnection();
            return response.status(200).json({"examiners":examiners});
        }
        else{
            closeConnection();
            return response.status(400).json({"error":"no examiners found"});
        }
    });

});

route.get('/admin/examiners/:id',[
    param('id','id is required to search examiner by id').notEmpty().trim().escape(),
    param('id','id should be of correct format').isMongoId()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }

    var query = {"_id":mongo.ObjectID(request.params.id)};

    collection.find(query,{ projection: { password: 0 } }).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no examiner found with id "+request.params.id});
        }

        var examiner = res[0];
        if(examiner.role==='examiner'){
            closeConnection();
            return response.status(200).json(examiner);
        }
        else{
            closeConnection();
            return response.status(400).json({"error":"no examiner found with id "+request.params.id});
        }
    });
});

route.get('/admin/examiners/:id/qrToken',[
    param('id','id is required to search examiner by id').notEmpty().trim().escape(),
    param('id','id should be of correct format').isMongoId()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }

    var query = {"_id":mongo.ObjectID(request.params.id)};

    collection.find(query,{ projection: { password: 0 } }).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no examiner found with id "+request.params.id});
        }

        var examiner = res[0];
        if(examiner.role==='examiner'){
            //examiner.exp = Math.floor(Date.now() / 1000) + (60 * 60 * 24);
            examiner.exp = Math.floor(Date.now() / 1000) + (60 * 5);
            var token = jwt.sign(examiner, tokenSecret);
            closeConnection();
            return response.status(200).json({"qrToken":token});
        }
        else{
            closeConnection();
            return response.status(400).json({"error":"no examiner found with id "+request.params.id});
        }
    });
});

route.get('/admin/teams/:id/qrToken',[
    param('id','id is required to search examiner by id').notEmpty().trim().escape(),
    param('id','id should be of correct format').isMongoId()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }

    var query = {"_id":mongo.ObjectID(request.params.id)};

    teamsCollection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no team found"});
        }

        var team = {
            id: res[0]._id
        };
        //team.exp = Math.floor(Date.now() / 1000) + (60 * 60 * 24);
        team.exp = Math.floor(Date.now() / 1000) + (60 * 5);
        var token = jwt.sign(team, tokenSecret);
        closeConnection();
        return response.status(200).json({"qrToken":token});
    });
});

route.get("/users/profile",verifyToken,(request,response)=>{
    var query = {"_id":new mongo.ObjectID(decoded._id)};
    collection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no user found with id "+decoded._id});
        }
        var user = new User(res[0]).getUser();
        closeConnection();
        return response.status(200).json(user);
    });
});

route.post('/examiner/score',[
    body('teamId','teamId required for creating team score').notEmpty().trim().escape(),
    body('teamId','teamId should be of a valid format').isMongoId(),
    body('scores','scores needed for team evaluation').notEmpty(),
    body('scores','scores should be present is an array').isArray()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }

    var scores = request.body.scores;
    var quetionIds = [];
    var total = 0;

    if(scores.length!=7){
        closeConnection();
        return response.status(400).json({"error":"incorrect number of questions submitted"});
    }

    var rawdata = fs.readFileSync('questions.json');
    if(!rawdata){
        closeConnection();
        return response.status(400).json({"error":"No questions file found"});
    }
    var questions = JSON.parse(rawdata);

    for(var i=0;i<scores.length;i++){
        if(typeof scores[i].id==='undefined' || scores[i].id===null || typeof scores[i].marks==='undefined' ||  scores[i].marks===null){
            closeConnection();
            return response.status(400).json({"error":"id or score not mentioned for some questions"});
        }

        if(!Number.isInteger(scores[i].id) || !Number.isInteger(scores[i].marks)){
            closeConnection();
            return response.status(400).json({"error":"some id or marks is not an integer value"});
        }

        var question = questions.results.find(it=>it.id==scores[i].id);
        if(!question){
            closeConnection();
            return response.status(400).json({"error":"question with id "+scores[i].id+" not found"});
        }
        var item = quetionIds.find(id=>id==scores[i].id);
        if(item){
            closeConnection();
            return response.status(400).json({"error":"some questions have been evaluated more than once"});
        }

        if(scores[i].marks<0 || scores[i].marks>4){
            closeConnection();
            return response.status(400).json({"error":"scores can have minimum 0 and maximum 4 value"});
        }

        quetionIds.push(scores[i].id);
        total = total + scores[i].marks;
    }

    var query = {'_id':mongo.ObjectID(request.body.teamId)};

    teamsCollection.find(query).toArray((error,resp)=>{
        if(error){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(resp.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no team found with id "+request.body.teamId});
        }

        var team = resp[0];
        var examiners = [];
        examiners = team.examiners;
        var msg="";

        var examinerScore = examiners.find(exam=>exam.examinerId==decoded._id);
        var updatedData={};
        var newExaminer = {
            "examinerId":decoded._id,
            "examinerFirstName":decoded.firstName,
            "examinerLastName":decoded.lastName,
            "score":total,
            "questions":request.body.scores
        };
        if(!examinerScore){
            var newAverage = ((parseFloat(team.averageScore)*examiners.length)+total)/(examiners.length+1);
            examiners.push(newExaminer);
            updatedData = {
                "averageScore":newAverage.toFixed(3),
                "examiners":examiners
            };
            msg="score added";
        }
        else{
            var idx = examiners.indexOf(examinerScore);
            var previousScore = (parseFloat(team.averageScore)*examiners.length)-examinerScore.score;
            var newAverage = (previousScore+total)/examiners.length;
            examiners.splice(idx,1);
            examiners.push(newExaminer);
            updatedData = {
                "averageScore":newAverage.toFixed(3),
                "examiners":examiners
            };
            msg="score updated";
        }

        var newQuery = {$set : updatedData};

        teamsCollection.updateOne(query,newQuery,(err,res)=>{
            if(err){
                closeConnection();
                return response.status(400).json({"error":"score could not be added/updated"});
            }
            else{
                closeConnection();
                return response.status(200).json({"result":msg});
            }
        });
    });
});




module.exports = route; 