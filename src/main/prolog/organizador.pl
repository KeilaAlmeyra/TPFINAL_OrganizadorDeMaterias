
% ==========================================
% ORGANIZADOR DE ESPACIOS UNIVERSITARIOS
% ==========================================

% Hechos:
% espacio(ID, Tipo, Capacidad, Estado).
% Java reemplaza estos hechos de ejemplo por los datos actuales de H2
% antes de realizar cada consulta.

:- dynamic espacio/4.
:- dynamic ocupacion/4.

espacio(aula101, aula, 30, disponible).
espacio(aula102, aula, 50, disponible).
espacio(aula103, aula, 20, disponible).

espacio(lab1, laboratorio, 25, disponible).
espacio(lab2, laboratorio, 40, disponible).

espacio(computacion1, computadoras, 30, disponible).
espacio(computacion2, computadoras, 20, mantenimiento).


% ==========================================
% REGLAS DE COMPATIBILIDAD
% ==========================================

% Un espacio es compatible si:
% 1. Tiene el tipo solicitado.
% 2. Está disponible.
% 3. Tiene capacidad suficiente.

espacio_compatible(ID, Tipo, Alumnos) :-
    espacio(ID, Tipo, Capacidad, disponible),
    Capacidad >= Alumnos.


% ==========================================
% OCUPACION DE ESPACIOS
% ==========================================

% ocupacion(Espacio, Dia, HoraInicio, HoraFin).

% Los horarios se expresan como minutos desde las 00:00.
ocupacion(aula101, lunes, 600, 720).
ocupacion(aula102, lunes, 840, 960).
ocupacion(lab1, martes, 540, 660).


% ==========================================
% DETECCION DE CONFLICTOS HORARIOS
% ==========================================

% Dos horarios se superponen cuando:
% el inicio del primero es anterior al fin del segundo
% y el inicio del segundo es anterior al fin del primero.

hay_conflicto(HoraInicio1, HoraFin1, HoraInicio2, HoraFin2) :-
    HoraInicio1 < HoraFin2,
    HoraInicio2 < HoraFin1.


% Comprueba si un espacio está ocupado
% en el día y horario solicitado.

espacio_ocupado(Espacio, Dia, HoraInicio, HoraFin) :-
    ocupacion(Espacio, Dia, InicioOcupacion, FinOcupacion),
    hay_conflicto(HoraInicio, HoraFin, InicioOcupacion, FinOcupacion).


% ==========================================
% BUSQUEDA DE ESPACIOS DISPONIBLES
% ==========================================

% Un espacio está disponible para una solicitud
% si es compatible y no tiene conflictos horarios.

espacio_disponible(ID, Tipo, Alumnos, Dia, HoraInicio, HoraFin) :-
    espacio_compatible(ID, Tipo, Alumnos),
    \+ espacio_ocupado(ID, Dia, HoraInicio, HoraFin).


% ==========================================
% DISPONIBILIDAD PARA VARIOS DIAS
% ==========================================

% Un espacio debe ser compatible y estar libre en TODOS los dias
% solicitados, dentro del intervalo horario indicado.
% Dias es una lista de atomos, por ejemplo [lunes,miercoles].
% Las horas se expresan como minutos desde las 00:00.

espacio_disponible_dias(ID, Tipo, Alumnos, Dias, HoraInicio, HoraFin) :-
    espacio_compatible(ID, Tipo, Alumnos),
    forall(
        member(Dia, Dias),
        \+ espacio_ocupado(ID, Dia, HoraInicio, HoraFin)
    ).
