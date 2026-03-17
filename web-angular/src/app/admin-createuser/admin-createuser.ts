import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SupabaseService } from '../supabase.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-createuser',
  imports: [FormsModule, CommonModule],
  standalone: true,
  templateUrl: './admin-createuser.html',
  styleUrl: './admin-createuser.css',
})
export class AdminCreateuser {
  username: string = "";
  password: string = "";
  password2: string = "";
  chosenRol: string = 'N';
  chosenRolDisplay: string = "- Sin Rol -"

  error: string = "";
  success: string = "";

  constructor(private supabaseService: SupabaseService) { }

  setRol(rol: string, str: string) {
    this.chosenRol = rol;
    this.chosenRolDisplay = str;
    console.log("cambiao")
  }

  async createNewUser() {
    this.error = ""
    this.success = ""
    // Usuario en blanco
    if (this.username == "") {
      this.error = "El usuario no puede estar en blanco!";
      return;
    }

    // Contraseña en blanco
    if (this.password == "") {
      this.error = "La contraseña no puede estar en blanco!";
      return;
    }

    // Sin rol
    if (this.chosenRol == 'N') {
      this.error = "Elige un rol para el usuario!";
      return;
    }

    // Contraseñas no coinciden
    if (this.password != this.password2) {
      this.error = "La contraseñas no coinciden!";
      return;
    }

    try {
      let match = await this.supabaseService.findUser(this.username.toLowerCase())
      if (match.length > 0) {
        this.error = `Ya existe el usuario ${this.username}!`
        return
      }
    } catch (e) {
      this.error = "Hubo un error de base de datos.."
      console.error(e)
      return;
    }

    await this.supabaseService.createUser(this.username.toLowerCase(), this.password, this.chosenRol)
    this.success = "Usuario creado!"
    console.log(`Usuario creado: ${this.username}`)
  }
}
