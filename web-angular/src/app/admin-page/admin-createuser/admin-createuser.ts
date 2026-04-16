import { Component, ElementRef, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SupabaseService } from '../../services/supabase.service';
import { CommonModule } from '@angular/common';
import { CarnetsConducir, Tablas, Usuario } from '../../services/tablas.supabase';
import { RouterLink } from "@angular/router";

@Component({
  selector: 'app-admin-createuser',
  imports: [FormsModule, CommonModule, RouterLink],
  standalone: true,
  templateUrl: './admin-createuser.html',
  styleUrl: './admin-createuser.scss',
})
export class AdminCreateuser {
  constructor(private supabaseService: SupabaseService) { }

  nombre: string = "";
  apellido1: string = "";
  apellido2: string = "";
  password: string = "";
  password2: string = "";
  tel: string = "";
  mail: string = "";
  avatar_path: string = "/img/pfp_default.jpg"

  // Rol
  chosenRol = { key: "N", display: "- Sin Rol -" };
  readonly roles = Object.entries({ N: '- Sin Rol -', A: 'Admin', U: 'Usuario', T: 'Trabajador' });

  // Carnet
  chosenCarnet: Record<string, boolean> = {
    "A1": false,
    "A2": false,
    "A": false,
    "B1": false,
    "B": false,
    "C1": false,
    "C": false,
    "D1": false,
    "D": false,
    "B+E": false,
    "C+E": false,
    "D+E": false,
  }
  readonly carnetLetras = Object.keys(CarnetsConducir)

  errorMsg = signal('');
  success = signal('');

  // Avatar
  imagenSeleccionada: File | null = null;
  imagenPreview = signal<string | null>(null);
  modalimagen = signal(false);
  cargandoImagen = signal(false);

  setRol(key: string, display: string) {
    this.chosenRol = { key, display }
  }

  setCarnet(letra: string) {
    this.chosenCarnet[letra] = !this.chosenCarnet[letra];
  }

  private validar(): string {
    if (!this.nombre) return 'El nombre no puede estar en blanco!';
    if (!this.apellido1) return 'El primer apellido no puede estar en blanco!';
    if (!this.password) return 'La contraseña no puede estar en blanco!';
    if (!this.tel) return 'El telefono no puede estar en blanco!';
    if (!this.mail) return 'El correo no puede estar en blanco!';
    if (this.chosenRol.key == 'N') return 'Elige un rol para el usuario!';
    if (this.password != this.password2) return 'Las contraseñas no coinciden!';
    return '';
  }

  async createNewUser() {
    this.errorMsg.set('')
    this.success.set('')

    const validationError = this.validar()
    if (validationError != '') {
      this.errorMsg.set(validationError);
      return;
    }

    try {
      const match = await this.supabaseService.find(Tablas.USUARIOS, "email", this.mail)
      if (match.length > 0) {
        this.errorMsg.set(`Ya existe un usuario con el correo '${this.mail}'!`);
        return;
      }

      //Creado correctamente
      const nuevo_usuario = new Usuario({
        nombre: this.nombre,
        apellido1: this.apellido1,
        apellido2: this.apellido2,
        password: this.password,
        rol: this.chosenRol.key,
        telefono: this.tel,
        email: this.mail,
        licencia_conducir: Object.keys(this.chosenCarnet).filter(key => this.chosenCarnet[key])
      })

      await this.supabaseService.insert(Tablas.USUARIOS, nuevo_usuario);

      if (this.imagenSeleccionada) {
        try {
          this.cargandoImagen.set(true);
          await this.supabaseService.subirAvatarUsuario("email", this.mail, this.imagenSeleccionada);
          this.success.set("Usuario con avatar creado!");
        } catch (e) {
          console.error("Error al subir avatar:", e);
          this.success.set("Usuario creado pero hubo error al subir el avatar");
        } finally {
          this.cargandoImagen.set(false);
        }
      } else {
        this.success.set("Usuario creado!");
      }


    } catch (e) {
      this.errorMsg.set("Hubo un error de base de datos..");
      console.error(e);
    }
  }

  // AVATAR

  cerrarModalImagen() {
    this.modalimagen.set(false)
  }

  // Muestra la imagen en el form
  confirmarImagen() {
    if (this.imagenSeleccionada && this.imagenPreview()) {
      this.avatar_path = this.imagenPreview()!;
      this.modalimagen.set(false);
    }
  }

  onArchivoSeleccionado(event: Event) {
    console.log("clic")
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];

    if (!archivo)
      return;

    // Validar que sea una imagen
    if (!archivo.type.startsWith('image/')) {
      this.errorMsg.set('Selecciona una imagen válida');
      return;
    }

    // Validar tamaño (maximo 5MB)
    const maxSizeMB = 5;
    if (archivo.size > maxSizeMB * 1024 * 1024) {
      this.errorMsg.set(`La imagen no puede superar ${maxSizeMB}MB`);
      return;
    }

    this.errorMsg.set('');
    this.imagenSeleccionada = archivo;

    // Mostrar preview en el modal
    const reader = new FileReader();
    reader.onload = (e) => {
      const preview = e.target?.result as string;
      this.imagenPreview.set(preview);
      this.modalimagen.set(true);
    };
    reader.readAsDataURL(archivo);
  }
}
