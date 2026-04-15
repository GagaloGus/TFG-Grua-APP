import { Injectable } from '@angular/core';
import { SupabaseService } from '../supabase.service';
import { Tablas, Usuario } from '../tablas.supabase';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private supabaseService: SupabaseService) { }

  private _isLoggedIn = false;
  private _isAdmin = false;

  private _currentUsuario! : Usuario | null;

  async login(email: string, password: string): Promise<string> {
    if(email == "")
      return "El campo 'Correo' esta vacio!"
    if(password == "")
      return "El campo 'Contraseña' esta vacio!"

    try {
      let match = await this.supabaseService.find(Tablas.USUARIOS, "email", email);

      //No existe el usuario
      if (match.length == 0) {
        throw Error(`No existe el usuario '${email}'`);
      }

      //Si todo encaja, se valida el login
      if (match[0]["email"] == email && match[0]["password"] == password) {

        //Asigna los valores que necesitamos a variables
        this._currentUsuario = new Usuario(match[0]);
        this._isLoggedIn = true
      }
      else{
        throw Error('La contraseña no coincide!')
      }
    } catch (error : any) {
      return error.message
    }

    return ''
  }

  isLoggedIn(): boolean {
    return this._isLoggedIn
  }

  isAdmin(): boolean {
    return this._isAdmin
  }

  logout() {
    this._currentUsuario = null;
    this._isLoggedIn = false
    this._isAdmin = false
  }

  get nombre() {
    if(this._currentUsuario == null)
      return null
    return this._currentUsuario.nombre == "" ? null : this._currentUsuario.nombre
  }

  get email() {
    if(this._currentUsuario == null)
      return null
    return this._currentUsuario.email == "" ? null : this._currentUsuario.email
  }

  get rol(){
    if(this._currentUsuario == null)
      return null
    return this._currentUsuario.rol;
  }
}
