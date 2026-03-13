import { Injectable } from '@angular/core';
import { SupabaseService } from '../../supabase.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private supabaseService: SupabaseService) {}

  private _isLoggedIn = false;
  private _isAdmin = false;
  data: any[] = [];

  private _currentID = "";
  private _currentUsername = "";
  private _currentPassword = "";

  // createUser(username: string, password: string): boolean{
  //   if(username in this.UsersLoginInfo)
  //     return false;

  //   if(username == "" || password == "")
  //     return false

  //   this.UsersLoginInfo[username] = password

  //   return this.login(username, password);
  // }

  async login(username: string, password: string): Promise<boolean> {
    this.data = await this.supabaseService.getAllUsers();

    //Recorre toda la lista de usuarios
    for (let i = 0; i < this.data.length; i++) {
      let fila = this.data[i];
      //Imprime por consola los valores
      console.log(`${fila["id"]} / ${fila["user"]} / ${fila["password"]} / ${fila["isadmin"]}`)

      //Asigna los valores que necesitamos a variables
      let id = fila["id"].toString()
      let usern = fila["user"].toString()
      let pswd = fila["password"].toString()
      let admin = fila["isadmin"]

      //Si todo encaja, se valida el login
      if(usern == username && pswd == password){
        this._currentID = id
        this._currentUsername = usern
        this._currentPassword = pswd
        this._isAdmin = admin
        this._isLoggedIn = true

        return true
      }
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
