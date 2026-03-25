import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SupabaseService } from '../../services/supabase.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-createuser',
  imports: [FormsModule, CommonModule],
  standalone: true,
  templateUrl: './admin-createuser.html',
  styleUrl: './admin-createuser.scss',
})
export class AdminCreateuser {
  constructor(private supabaseService: SupabaseService) { }

  username: string = "";
  password: string = "";
  password2: string = "";
  tel: string = "";

  // Rol
  chosenRol = { key: "N", display: "- Sin Rol -" };
  readonly roles = Object.entries({ N: '- Sin Rol -', A: 'Admin', U: 'Usuario', T: 'Trabajador' });

  // Carnet
  chosenCarnet: Record<string, boolean> = { B: false, C: false, E: false }
  readonly carnetLetras = Object.keys(this.chosenCarnet)

  error: string = "";
  success: string = "";

  setRol(key: string, display: string) {
    this.chosenRol = { key, display }
  }

  setCarnet(letra: string) {
    this.chosenCarnet[letra] = !this.chosenCarnet[letra];
  }

  private validar(): string {
    if (!this.username) return 'El usuario no puede estar en blanco!';
    if (!this.password) return 'La contraseña no puede estar en blanco!';
    if (this.chosenRol.key == 'N') return 'Elige un rol para el usuario!';
    if (this.password != this.password2) return 'Las contraseñas no coinciden!';
    return '';
  }

  async createNewUser() {
    this.error = ""
    this.success = ""

    const validationError = this.validar()
    if (validationError != '') {
      this.error = validationError;
      return;
    }

    try {
      const match = await this.supabaseService.findUser(this.username.toLowerCase())
      if (match.length > 0) {
        this.error = `Ya existe el usuario ${this.username}!`;
        return;
      }

      //Creado correctamente
      const carnets = Object.keys(this.chosenCarnet).filter(key=> this.chosenCarnet[key])
      await this.supabaseService.createUser(this.username.toLowerCase(), this.password, this.chosenRol.key, this.tel, carnets)
      this.success = "Usuario creado!";
      console.log(`Usuario creado: ${this.username}`);

    } catch (e) {
      this.error = "Hubo un error de base de datos..";
      console.error(e);
    }
  }
}
