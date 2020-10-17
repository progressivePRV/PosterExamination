class User{
    constructor(user){
        this._id=user._id;
        this.firstName=user.firstName;
        this.lastName=user.lastName;
        this.email=user.email;
        this.role=user.role;
        this.password=user.password;
    }

    getUser(){
        var usr = {
            "_id":this._id,
            "firstName":this.firstName,
            "lastName":this.lastName,
            "email":this.email,
            "role":this.role
        }

        return usr;
    }
}
module.exports = User;