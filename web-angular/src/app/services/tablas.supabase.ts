export enum Tablas {
  USUARIOS = 'usuarios',
  VEHICULOS = 'vehiculos',
  SERVICIOS = 'servicios'
}

export class Servicio {
  id!: number;
  ubicacion_recogida!: number[];
  ubicacion_destino!: number[];
  fecha!: string;
  operador_id!: string | null;
  vehiculo_id!: number | null;
  observaciones!: string | null;
  estado!: string;
  tel_cliente!: string | null;
  nombre_cliente!: string | null;

  constructor(data: Partial<Servicio>) {
    Object.assign(this, data);
  }
}

export class Usuario {
  id!: number;
  num_empleado!: number;
  created_at?: string;
  nombre!: string;
  apellido1!: string;
  apellido2!: string;
  password!: string;
  rol!: string;
  disponibilidad!: string;
  serv_completados_total!: number;
  serv_completados_hoy!: number;
  licencia_conducir!: string[] | null;
  telefono!: string | null;
  email!: string | null;

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
    if(this.disponibilidad == "En servicio") return "chip-text-orange"
    else if(this.disponibilidad == "Disponible") return "chip-text-green"
    else if(this.disponibilidad == "Inactivo") return "chip-text-grey"
    return "chip-text"
  }

  get rolNombre():string{
    if(this.rol == "N") return "Sin rol"
    else if(this.rol == "A") return "ADMIN"
    else if(this.rol == "U") return "Usuario"
    else if(this.rol == "T") return "Trabajador"
    return "Rol indefinido"
  }

    get rolCSSClass():string{
    if(this.rol == "N") return "chip-text-grey"
    else if(this.rol == "A") return "chip-text-purple"
    else if(this.rol == "U") return "chip-text-blue"
    else if(this.rol == "T") return "chip-text-red"
    return "chip-text"
  }
}