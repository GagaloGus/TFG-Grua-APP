import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private _isLoggedIn = false;
  private _isAdmin = false;

  private _currentUsername = "";
  private _currentPassword = "";

  private AdminUsername = "admin"
  private AdminPassword = "1234"

  private UsersLoginInfo: { [username: string]: string } = {
    usuario1: "1234"
  };

  createUser(username: string, password: string): boolean{
    if(username in this.UsersLoginInfo)
      return false;

    if(username == "" || password == "")
      return false

    this.UsersLoginInfo[username] = password

    return this.login(username, password);
  }

  login(username: string, password: string): boolean {
    // Administrador
    if (username == this.AdminUsername && password == this.AdminPassword) {
      this._isAdmin = true
      this._isLoggedIn = true

      this._currentUsername = username
      this._currentPassword = password

      return true
    }
    // Usuario normal
    else if(username in this.UsersLoginInfo && this.UsersLoginInfo[username] == password){
      this._isLoggedIn = true
      
      this._currentUsername = username
      this._currentPassword = password

      return true
    }
    
    return false
  }
  
  isLoggedIn(): boolean{
    return this._isLoggedIn
  }
  
  isAdmin():boolean{
    return this._isAdmin
  }

  logout() {
    this._isLoggedIn = false
    this._isAdmin = false
  }


  getUsername():string{
    return this._currentUsername
  }

  getPassword():string{
    return this._currentPassword
  }
}
