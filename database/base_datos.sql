-- -------------------------------------------------------------------------------------
-- BASE DE DATOS GRUA APP

-- TABLA: usuaruios

create table usuarios (
--usamos auth.users para que coincida con el sistema de login
    id uuid references auth.users(id) on delete cascade primary key,
    nombre varchar(100) not null,
    apellido1 varchar(100),
    apellido2 varchar(100),
    telefono varchar(15),
    email varchar(150) unique not null,
    rol varchar (20) not null check (rol in ('admin','operador')),
    activo boolean default true,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- TABLA vehiculos

create table vehiculos (
    id bigserial primary key,
    matricula varchar(20) unique not null,
    disponible boolean default true,
    zona_trabajo varchar(100),
    operador_id uuid, -- Referencia en tabla de usuarios
    activo boolean default true,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,

    constraint fk_vehiculo_operador
        foreign key (operador_id)
        references usuarios(id)
        on delete set null
);

-- TABLA servicios

create table servicios (
    id bigserial primary key,
    direccion_recogida text not null,
    destino text not null,
    estado varchar(20) not null default 'pendiente'
        check (estado in ('pendiente', 'en_curso', 'terminado', 'cancelado')),
-- para que supabase maneje bien las zonas horarias
    fecha timestamp with time zone default timezone('utc'::text, now()) not null,
    operador_id uuid,
    vehiculo_id bigint,
    observaciones text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,

    constraint fk_servicio_operador
        foreign key (operador_id) references usuarios(id)
        on delete set null,
    constraint fk_servicio_vehiculo
        foreign key (vehiculo_id) references vehiculos(id)
        on delete set null
);