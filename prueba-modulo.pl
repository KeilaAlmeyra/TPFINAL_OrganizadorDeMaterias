:- consult('C:/java/TPFINAL_OrganizadorDeMaterias/src/main/prolog/organizador.pl').

:- initialization(main).

main :-
    current_predicate(espacio_disponible_dias/6),
    listing(espacio_disponible_dias/6),
    halt.
