import { error } from "console";

export class Usuario {
  id!: number;
  created_at?: string;
  nombre!: string;
  apellido1!: string;
  apellido2!: string;
  password!: string;
  role!: string;
  disponible!: string;
  serv_completados_total!: number;
  serv_completados_hoy!: number;
  licencia_conducir!: string[] | null;
  tel!: string | null;
  mail!: string | null;

  constructor(data: Partial<Usuario>) {
    Object.assign(this, data);
  }

  get nombreCompleto(): string {
    return `${this.nombre} ${this.apellido1} ${this.apellido2}`.trim();
  }

  get licenciasFormateadas(): string {
    return this.licencia_conducir?.join(' + ') ?? 'Sin licencia';
    // → "B + C" o "Sin licencia"
  }

  get disponibilidadCSSClass():string{
    if(this.disponible == "En servicio") return "chip-text-orange"
    else if(this.disponible == "Disponible") return "chip-text-green"
    else if(this.disponible == "Inactivo") return "chip-text-grey"
    return "chip-text"
  }

  get rolNombre():string{
    if(this.role == "N") return "Sin rol"
    else if(this.role == "A") return "ADMIN"
    else if(this.role == "U") return "Usuario"
    else if(this.role == "T") return "Trabajador"
    return "Rol indefinido"
  }

    get rolCSSClass():string{
    if(this.role == "N") return "chip-text-grey"
    else if(this.role == "A") return "chip-text-purple"
    else if(this.role == "U") return "chip-text-blue"
    else if(this.role == "T") return "chip-text-red"
    return "chip-text"
  }
}