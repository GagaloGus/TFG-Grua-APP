export enum Tablas {
  USUARIOS = 'usuarios',
  VEHICULOS = 'vehiculos',
  SERVICIOS = 'servicios'
}

export class Vehiculo {
  id!: number;
  matricula!: string;
  disponible!: boolean;
  activo!: boolean;
  marca!: string | null;
  zona_trabajo!: string | null;

  constructor(data: Partial<Vehiculo> = {}) {
    Object.assign(this, data);
  }

  static empty(): Vehiculo {
    return new Vehiculo({
      disponible: false,
      activo: false,
      marca: null,
      zona_trabajo: null,
    });
  }
}

export class Servicio {
  id!: number;
  direccion_ubi_recogida!: string | null;
  ubicacion_recogida_lat!: number;
  ubicacion_recogida_lng!: number;
  direccion_ubi_destino!: string | null;
  ubicacion_destino_lat!: number;
  ubicacion_destino_lng!: number;
  fecha!: string;
  costo: number = 0;
  num_empleado!: number | null;
  vehiculo_matricula!: string | null;
  observaciones!: string | null;
  estado!: string;
  tel_cliente!: string | null;
  nombre_cliente!: string | null;

  constructor(data: Partial<Servicio> = {}) {
    Object.assign(this, data);
  }

  static empty(): Servicio {
    return new Servicio({
      direccion_ubi_recogida: null,
      ubicacion_recogida_lat: 0,
      ubicacion_recogida_lng: 0,
      direccion_ubi_destino: null,
      ubicacion_destino_lat: 0,
      ubicacion_destino_lng: 0,
      estado: "Sin empezar",
      num_empleado: null,
      vehiculo_matricula: null,
      observaciones: null,
      tel_cliente: null,
      nombre_cliente: null,
    });
  }

  get badgeClass(): string {
    const map: Record<string, string> = {
      'Sin empezar': 'badge-estado-sin-empezar',
      'En curso': 'badge-estado-en-curso',
      'Terminado': 'badge-estado-terminado',
      'Cancelado': 'badge-estado-cancelado',
    };
    return map[this.estado] ?? '';
  }

  get formatCoordRecogida(): string {
    return `${this.ubicacion_recogida_lat.toFixed(4)}, ${this.ubicacion_recogida_lng.toFixed(4)}`
  }

  get formatCoordDestino(): string {
    return `${this.ubicacion_destino_lat.toFixed(4)}, ${this.ubicacion_destino_lng.toFixed(4)}`
  }
}

export class Usuario {
  id!: number;
  num_empleado!: number;
  vehiculo_asignado!: string | null;
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

  constructor(data: Partial<Usuario> = {}) {
    Object.assign(this, data);
  }

  static empty(): Usuario {
    return new Usuario({});
  }

  get nombreCompleto(): string {
    return `${this.nombre ?? ""} ${this.apellido1 ?? ""} ${this.apellido2 ?? ""}`.trim();
  }

  get licenciasFormateadas(): string {
    // "B + C" o "Sin licencia"
    return this.licencia_conducir?.join('+') ?? 'Sin licencia';
  }

  get disponibilidadCSSClass(): string {
    if (this.disponibilidad == "En servicio") return "chip-text-blue"
    else if (this.disponibilidad == "Disponible") return "chip-text-green"
    else if (this.disponibilidad == "Inactivo") return "chip-text-grey"
    return "chip-text"
  }

  get rolNombre(): string {
    if (this.rol == "N") return "Sin rol"
    else if (this.rol == "A") return "ADMIN"
    else if (this.rol == "U") return "Usuario"
    else if (this.rol == "T") return "Trabajador"
    return "Rol indefinido"
  }

  get rolCSSClass(): string {
    if (this.rol == "N") return "chip-text-grey"
    else if (this.rol == "A") return "chip-text-purple"
    else if (this.rol == "U") return "chip-text-blue"
    else if (this.rol == "T") return "chip-text-red"
    return "chip-text"
  }
}