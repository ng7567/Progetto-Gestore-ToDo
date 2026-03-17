--
-- PostgreSQL database dump
--

\restrict i8CRWpwJ1E2H5Sf9bBt7cgOmepy5boWMaHBoALE50klA1KbxbL32mkAq5AhxwSM

-- Dumped from database version 17.6
-- Dumped by pg_dump version 17.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: priority_level; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.priority_level AS ENUM (
    'ALTA',
    'MEDIA',
    'BASSA'
);


ALTER TYPE public.priority_level OWNER TO postgres;

--
-- Name: check_and_create_board_on_move(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.check_and_create_board_on_move() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    new_board_title VARCHAR(100);
    collaborator_record RECORD;
BEGIN
    IF NEW.board_id = OLD.board_id THEN RETURN NEW; END IF;

    -- 1. Trova titolo nuova bacheca
    SELECT title INTO new_board_title FROM boards WHERE board_id = NEW.board_id;

    -- 2. Cicla sui collaboratori
    FOR collaborator_record IN 
        SELECT user_id FROM shared_todos WHERE todo_id = NEW.todo_id
    LOOP
        -- 3. Crea bacheca se manca
        IF NOT EXISTS (
            SELECT 1 FROM boards 
            WHERE user_id = collaborator_record.user_id AND title = new_board_title
        ) THEN
            INSERT INTO boards (title, user_id) VALUES (new_board_title, collaborator_record.user_id);
        END IF;
    END LOOP;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.check_and_create_board_on_move() OWNER TO postgres;

--
-- Name: check_and_create_board_on_share(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.check_and_create_board_on_share() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    origin_board_title VARCHAR(100);
    target_has_board BOOLEAN;
BEGIN
    -- 1. Trova il titolo della bacheca originale
    SELECT b.title INTO origin_board_title
    FROM todos t JOIN boards b ON t.board_id = b.board_id
    WHERE t.todo_id = NEW.todo_id;

    -- 2. Controlla se il destinatario ha gi… la bacheca
    SELECT EXISTS (
        SELECT 1 FROM boards 
        WHERE user_id = NEW.user_id AND title = origin_board_title
    ) INTO target_has_board;

    -- 3. Se no, creala
    IF NOT target_has_board THEN
        INSERT INTO boards (title, user_id) VALUES (origin_board_title, NEW.user_id);
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.check_and_create_board_on_share() OWNER TO postgres;

--
-- Name: check_shared_todo_position_logic(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.check_shared_todo_position_logic() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_target_board_title VARCHAR(100);
    v_target_board_id INTEGER;
BEGIN
    -- Recupera il titolo della bacheca originale
    SELECT b.title INTO v_target_board_title
    FROM public.todos t
    JOIN public.boards b ON t.board_id = b.board_id
    WHERE t.todo_id = NEW.todo_id;

    -- Trova l'ID della bacheca per l'utente che riceve la condivisione
    SELECT board_id INTO v_target_board_id
    FROM public.boards
    WHERE title = v_target_board_title AND user_id = NEW.user_id;

    -- Esegue lo scorrimento solo se la bacheca di destinazione esiste
    IF v_target_board_id IS NOT NULL THEN
        -- Sposta in avanti di una posizione i task proprietari del ricevente
        UPDATE public.todos
        SET position_order = position_order + 1
        WHERE board_id = v_target_board_id;

        -- Sposta in avanti di una posizione i task gi… condivisi con il ricevente in quella bacheca
        UPDATE public.shared_todos st
        SET position_order = st.position_order + 1
        FROM public.todos t
        JOIN public.boards b ON t.board_id = b.board_id
        WHERE st.todo_id = t.todo_id
          AND st.user_id = NEW.user_id
          AND b.title = v_target_board_title;
    END IF;

    -- Imposta il nuovo task condiviso forzatamente in prima posizione
    NEW.position_order := 1;

    -- Restituisce il record pronto per l'inserimento
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.check_shared_todo_position_logic() OWNER TO postgres;

--
-- Name: check_todo_position_logic(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.check_todo_position_logic() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_max_pos INTEGER;
    v_owner_id INTEGER;
BEGIN
    -- Recupera l'ID del proprietario della bacheca
    SELECT user_id INTO v_owner_id FROM public.boards WHERE board_id = NEW.board_id;

    -- Calcola il valore massimo attuale tramite la vista unificata
    SELECT COALESCE(MAX(position_order), 0) INTO v_max_pos
    FROM public.v_user_visible_todos
    WHERE board_id = NEW.board_id AND viewer_id = v_owner_id;

    -- Imposta il nuovo task proprietario rigorosamente in coda
    NEW.position_order := v_max_pos + 1;

    -- Restituisce il record modificato senza alterare altre righe
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.check_todo_position_logic() OWNER TO postgres;

--
-- Name: create_default_boards(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.create_default_boards() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    INSERT INTO boards (title, description, user_id) VALUES ('Universit' || U&'\00E0', 'Gestione dei corsi, esami da sostenere e scadenze dei progetti universitari.', NEW.user_id);
    INSERT INTO boards (title, description, user_id) VALUES ('Lavoro', 'Task professionali, scadenze ufficio e obiettivi di carriera.', NEW.user_id);
    INSERT INTO boards (title, description, user_id) VALUES ('Tempo libero', 'Hobby, sport, viaggi e attivit… personali da svolgere nel tempo libero.', NEW.user_id);
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.create_default_boards() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: boards; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.boards (
    board_id integer NOT NULL,
    title character varying(100) NOT NULL,
    user_id integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    description text
);


ALTER TABLE public.boards OWNER TO postgres;

--
-- Name: shared_todos; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.shared_todos (
    todo_id integer NOT NULL,
    user_id integer NOT NULL,
    assigned_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    position_order integer DEFAULT 1 NOT NULL
);


ALTER TABLE public.shared_todos OWNER TO postgres;

--
-- Name: todos; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.todos (
    todo_id integer NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    expiry_date timestamp without time zone,
    url_link text,
    image_path text,
    background_color character varying(7) DEFAULT '#FFFFFF'::character varying,
    is_completed boolean DEFAULT false,
    position_order integer DEFAULT 1 NOT NULL,
    priority public.priority_level DEFAULT 'BASSA'::public.priority_level,
    board_id integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_expiry_after_created CHECK (((expiry_date >= (created_at - '00:01:00'::interval)) OR (expiry_date IS NULL)))
);


ALTER TABLE public.todos OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    user_id integer NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_hash_length CHECK ((length((password)::text) = 60)),
    CONSTRAINT check_username_format CHECK (((length((username)::text) >= 3) AND (length((username)::text) <= 20) AND ((username)::text ~ '^[a-zA-Z0-9_.-]+$'::text)))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: v_user_visible_todos; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_user_visible_todos AS
 SELECT t.todo_id,
    t.title,
    t.description,
    t.expiry_date,
    t.is_completed,
    t.priority,
    t.board_id,
    t.position_order,
    t.background_color,
    t.image_path,
    t.url_link,
    b.title AS board_title,
    u.user_id AS viewer_id,
    u.username AS owner_username,
    'OWNER'::text AS role
   FROM ((public.todos t
     JOIN public.boards b ON ((t.board_id = b.board_id)))
     JOIN public.users u ON ((b.user_id = u.user_id)))
UNION ALL
 SELECT t.todo_id,
    t.title,
    t.description,
    t.expiry_date,
    t.is_completed,
    t.priority,
    b_shared.board_id,
    st.position_order,
    t.background_color,
    t.image_path,
    t.url_link,
    b_shared.title AS board_title,
    st.user_id AS viewer_id,
    u_owner.username AS owner_username,
    'SHARED'::text AS role
   FROM ((((public.todos t
     JOIN public.shared_todos st ON ((t.todo_id = st.todo_id)))
     JOIN public.boards b_owner ON ((t.board_id = b_owner.board_id)))
     JOIN public.users u_owner ON ((b_owner.user_id = u_owner.user_id)))
     JOIN public.boards b_shared ON (((b_shared.user_id = st.user_id) AND ((b_shared.title)::text = (b_owner.title)::text))));


ALTER VIEW public.v_user_visible_todos OWNER TO postgres;

--
-- Name: fn_get_due_today(integer, character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_get_due_today(p_user_id integer, p_board_title character varying) RETURNS SETOF public.v_user_visible_todos
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Restituisce i record della vista filtrando per data corrente e titolo bacheca
    RETURN QUERY
    SELECT * FROM v_user_visible_todos
    WHERE viewer_id = p_user_id
    AND board_title = p_board_title
    AND expiry_date::DATE = CURRENT_DATE;
END;
$$;


ALTER FUNCTION public.fn_get_due_today(p_user_id integer, p_board_title character varying) OWNER TO postgres;

--
-- Name: fn_get_due_within(integer, character varying, date); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_get_due_within(p_user_id integer, p_board_title character varying, p_target_date date) RETURNS SETOF public.v_user_visible_todos
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Seleziona i todo con scadenza compresa tra oggi e la data fornita in input
    RETURN QUERY
    SELECT * FROM v_user_visible_todos
    WHERE viewer_id = p_user_id
    AND board_title = p_board_title
    AND expiry_date::DATE >= CURRENT_DATE 
    AND expiry_date::DATE <= p_target_date;
END;
$$;


ALTER FUNCTION public.fn_get_due_within(p_user_id integer, p_board_title character varying, p_target_date date) OWNER TO postgres;

--
-- Name: fn_search_todos(integer, character varying, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_search_todos(p_user_id integer, p_board_title character varying, p_query text) RETURNS SETOF public.v_user_visible_todos
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Restituisce i record applicando un filtro case-insensitive sulla bacheca indicata
    RETURN QUERY
    SELECT * FROM v_user_visible_todos
    WHERE viewer_id = p_user_id
    AND board_title = p_board_title
    AND (title ILIKE format('%%%s%%', p_query) 
         OR description ILIKE format('%%%s%%', p_query));
END;
$$;


ALTER FUNCTION public.fn_search_todos(p_user_id integer, p_board_title character varying, p_query text) OWNER TO postgres;

--
-- Name: leave_shares_on_board_delete(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.leave_shares_on_board_delete() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Elimina la mia partecipazione (shared_todos)
    -- per tutti i task che appartengono a un progetto con lo STESSO NOME
    -- della bacheca che sto cancellando.
    DELETE FROM shared_todos
    WHERE user_id = OLD.user_id  -- Sono io che sto cancellando
    AND todo_id IN (
        SELECT t.todo_id
        FROM todos t
        JOIN boards b ON t.board_id = b.board_id
        WHERE b.title = OLD.title -- Match per Titolo (il legame logico)
    );

    RETURN OLD;
END;
$$;


ALTER FUNCTION public.leave_shares_on_board_delete() OWNER TO postgres;

--
-- Name: prevent_collaborator_rename(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.prevent_collaborator_rename() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    shared_task_count INTEGER;
BEGIN
    -- Se il titolo non cambia, tutto ok
    IF NEW.title = OLD.title THEN 
        RETURN NEW; 
    END IF;

    -- Controlliamo se in questa bacheca ci sono task CONDIVISI (dove io sono il collaboratore, non l'owner)
    -- La logica Š: Esiste un task condiviso con ME (st.user_id = NEW.user_id)
    -- il cui proprietario originale ha dato alla bacheca lo STESSO NOME che sto provando a cambiare (OLD.title)
    SELECT COUNT(*) INTO shared_task_count
    FROM shared_todos st
    JOIN todos t ON st.todo_id = t.todo_id
    JOIN boards owner_board ON t.board_id = owner_board.board_id
    WHERE st.user_id = NEW.user_id        -- Io sono il collaboratore
    AND owner_board.title = OLD.title;    -- Il titolo originale corrisponde

    -- Se trovo anche solo un task che dipende da questo nome
    IF shared_task_count > 0 THEN
        RAISE EXCEPTION 'Impossibile rinominare: questa bacheca contiene task condivisi da altri utenti. Solo il proprietario pu• cambiare il nome del progetto.';
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.prevent_collaborator_rename() OWNER TO postgres;

--
-- Name: sync_board_title_on_rename(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.sync_board_title_on_rename() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    collaborator_id INT;
BEGIN
    -- Interrompe l'esecuzione se il titolo della bacheca non e variato
    IF NEW.title = OLD.title THEN 
        RETURN NEW; 
    END IF;

    -- Itera su tutti i collaboratori associati ai task della bacheca rinominata
    FOR collaborator_id IN
        SELECT DISTINCT st.user_id
        FROM todos t
        JOIN shared_todos st ON t.todo_id = st.todo_id
        WHERE t.board_id = NEW.board_id
    LOOP
        -- Inserisce la nuova bacheca per il collaboratore solo se non gia presente
        INSERT INTO boards (title, user_id)
        SELECT NEW.title, collaborator_id
        WHERE NOT EXISTS (
            SELECT 1 FROM boards 
            WHERE user_id = collaborator_id AND title = NEW.title
        );
    END LOOP;

    -- Restituisce il record bacheca aggiornato
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.sync_board_title_on_rename() OWNER TO postgres;

--
-- Name: update_timestamp(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_timestamp() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_timestamp() OWNER TO postgres;

--
-- Name: boards_board_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.boards_board_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.boards_board_id_seq OWNER TO postgres;

--
-- Name: boards_board_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.boards_board_id_seq OWNED BY public.boards.board_id;


--
-- Name: todos_todo_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.todos_todo_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.todos_todo_id_seq OWNER TO postgres;

--
-- Name: todos_todo_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.todos_todo_id_seq OWNED BY public.todos.todo_id;


--
-- Name: users_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_user_id_seq OWNER TO postgres;

--
-- Name: users_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_user_id_seq OWNED BY public.users.user_id;


--
-- Name: boards board_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.boards ALTER COLUMN board_id SET DEFAULT nextval('public.boards_board_id_seq'::regclass);


--
-- Name: todos todo_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.todos ALTER COLUMN todo_id SET DEFAULT nextval('public.todos_todo_id_seq'::regclass);


--
-- Name: users user_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN user_id SET DEFAULT nextval('public.users_user_id_seq'::regclass);


--
-- Data for Name: boards; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.boards (board_id, title, user_id, created_at, updated_at, description) FROM stdin;
\.


--
-- Data for Name: shared_todos; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.shared_todos (todo_id, user_id, assigned_at, position_order) FROM stdin;
\.


--
-- Data for Name: todos; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.todos (todo_id, title, description, expiry_date, url_link, image_path, background_color, is_completed, position_order, priority, board_id, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (user_id, username, password, created_at, updated_at) FROM stdin;
\.


--
-- Name: boards_board_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.boards_board_id_seq', 34, true);


--
-- Name: todos_todo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.todos_todo_id_seq', 88, true);


--
-- Name: users_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_user_id_seq', 14, true);


--
-- Name: boards boards_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.boards
    ADD CONSTRAINT boards_pkey PRIMARY KEY (board_id);


--
-- Name: shared_todos shared_todos_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.shared_todos
    ADD CONSTRAINT shared_todos_pkey PRIMARY KEY (todo_id, user_id);


--
-- Name: todos todos_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.todos
    ADD CONSTRAINT todos_pkey PRIMARY KEY (todo_id);


--
-- Name: boards unique_user_board_title; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.boards
    ADD CONSTRAINT unique_user_board_title UNIQUE (user_id, title);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: boards set_timestamp_boards; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER set_timestamp_boards BEFORE UPDATE ON public.boards FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();


--
-- Name: todos set_timestamp_todos; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER set_timestamp_todos BEFORE UPDATE ON public.todos FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();


--
-- Name: users set_timestamp_users; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER set_timestamp_users BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_timestamp();


--
-- Name: shared_todos trg_fix_shared_position_insert; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_fix_shared_position_insert BEFORE INSERT ON public.shared_todos FOR EACH ROW EXECUTE FUNCTION public.check_shared_todo_position_logic();


--
-- Name: todos trg_fix_todo_position_insert; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_fix_todo_position_insert BEFORE INSERT ON public.todos FOR EACH ROW EXECUTE FUNCTION public.check_todo_position_logic();


--
-- Name: users trigger_add_default_boards; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_add_default_boards AFTER INSERT ON public.users FOR EACH ROW EXECUTE FUNCTION public.create_default_boards();


--
-- Name: shared_todos trigger_ensure_board_exists; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_ensure_board_exists AFTER INSERT ON public.shared_todos FOR EACH ROW EXECUTE FUNCTION public.check_and_create_board_on_share();


--
-- Name: todos trigger_ensure_board_on_move; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_ensure_board_on_move AFTER UPDATE OF board_id ON public.todos FOR EACH ROW EXECUTE FUNCTION public.check_and_create_board_on_move();


--
-- Name: boards trigger_leave_shares; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_leave_shares BEFORE DELETE ON public.boards FOR EACH ROW EXECUTE FUNCTION public.leave_shares_on_board_delete();


--
-- Name: boards trigger_prevent_rename; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_prevent_rename BEFORE UPDATE OF title ON public.boards FOR EACH ROW EXECUTE FUNCTION public.prevent_collaborator_rename();


--
-- Name: boards trigger_sync_board_rename; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_sync_board_rename AFTER UPDATE OF title ON public.boards FOR EACH ROW EXECUTE FUNCTION public.sync_board_title_on_rename();


--
-- Name: boards boards_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.boards
    ADD CONSTRAINT boards_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: shared_todos shared_todos_todo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.shared_todos
    ADD CONSTRAINT shared_todos_todo_id_fkey FOREIGN KEY (todo_id) REFERENCES public.todos(todo_id) ON DELETE CASCADE;


--
-- Name: shared_todos shared_todos_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.shared_todos
    ADD CONSTRAINT shared_todos_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: todos todos_board_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.todos
    ADD CONSTRAINT todos_board_id_fkey FOREIGN KEY (board_id) REFERENCES public.boards(board_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict i8CRWpwJ1E2H5Sf9bBt7cgOmepy5boWMaHBoALE50klA1KbxbL32mkAq5AhxwSM

