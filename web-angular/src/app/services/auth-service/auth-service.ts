import { Injectable } from '@angular/core';
import { SupabaseService } from '../supabase.service';
import { Tablas } from '../tablas.supabase';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private supabaseService: SupabaseService) { }

  private _isLoggedIn = false;
  private _isAdmin = false;

  private _currentEmail = "";
  private _currentPassword = "";
  private _currentRole = "";


  async login(email: string, password: string): Promise<boolean> {
    try {
      let match = await this.supabaseService.find(Tablas.USUARIOS, "email", email);

      //No existe el usuario
      if (match.length == 0) {
        return false;
      }

      //Recorre toda la lista de usuarios
      let fila = match[0];
      //Imprime por consola los valores
      console.log(`${fila["id"]} / ${fila["email"]} / ${fila["password"]} / ${fila["role"]}`)

      //Asigna los valores que necesitamos a variables
      let _id = fila["id"].toString()
      let _email = fila["email"].toString()
      let _pswd = fila["password"].toString()
      let _role = fila["role"]

      //Si todo encaja, se valida el login
      if (_email == email && _pswd == password) {
        this._currentEmail = _email
        this._currentPassword = _pswd
        this._currentRole = _role
        this._isAdmin = _role == 'A';
        this._isLoggedIn = true

        return true
      }
    } catch (error) { }

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

  getEmail(): string {
    return this._currentEmail
  }

  getPassword(): string {
    return this._currentPassword
  }
}
