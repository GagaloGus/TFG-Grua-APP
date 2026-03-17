import { Injectable } from '@angular/core';
import { SupabaseService } from '../../supabase.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private supabaseService: SupabaseService) { }

  private _isLoggedIn = false;
  private _isAdmin = false;

  private _currentUsername = "";
  private _currentPassword = "";
  private _currentRole = "";


  async login(username: string, password: string): Promise<boolean> {
    try {
      let match = await this.supabaseService.findUser(username);

      //No existe el usuario
      if (match.length == 0) {
        return false;
      }

      //Recorre toda la lista de usuarios
      let fila = match[0];
      //Imprime por consola los valores
      console.log(`${fila["id"]} / ${fila["user"]} / ${fila["password"]} / ${fila["role"]}`)

      //Asigna los valores que necesitamos a variables
      let id = fila["id"].toString()
      let usern = fila["user"].toString()
      let pswd = fila["password"].toString()
      let role = fila["role"]

      //Si todo encaja, se valida el login
      if (usern == username && pswd == password) {
        this._currentUsername = usern
        this._currentPassword = pswd
        this._currentRole = role
        this._isAdmin = role == 'A';
        this._isLoggedIn = true

        return true
      }
    } catch (error) {

    }

    return false
  }

  isLoggedIn(): boolean {
    return this._isLoggedIn
  }

  isAdmin(): boolean {
    return this._isAdmin
  }

  logout() {
    this._isLoggedIn = false
    this._isAdmin = false
  }

  getUsername(): string {
    return this._currentUsername
  }

  getPassword(): string {
    return this._currentPassword
  }
}
