var tablaRoles;

$(document).ready(() => {
  tablaRoles = $("#tablaRoles").DataTable({
    responsive: true,    // activa el plugin responsive
    autoWidth: false,    // deshabilita el sizing automático
    scrollX: true,       // permite scroll horizontal si hiciera falta
    columns: [
      { data: "idRol" },
      { data: "tipoRol" },
       {
         data: "permisos",
         render: function(perms) {
           if (!perms || perms.length === 0) {
             return "";
           }
           // Usamos función normal en lugar de arrow
           return perms.map(function(p) {
             return p.nombre;
           }).join(", ");
         }
       },
      {
        data: null,
        render: row => `
          <button class="btn btn-sm btn-info" onclick="mostrarFormularioEditarRol(${row.idRol})">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn btn-sm btn-danger" onclick="eliminarRol(${row.idRol})">
            <i class="fas fa-trash"></i>
          </button>`
      }
    ]
  });

  cargarRoles();
});

function cargarRoles() {
  $.get("/roles/all", (data) => {
    tablaRoles.clear().rows.add(data).draw();
  });
}

function mostrarFormularioNuevoRol() {
  $.get("/roles/nuevo", (html) => {
    $("#modalRol").html(html);
    $("#modalFormRol").modal("show");
    bindFormularioRol();
  });
}

function mostrarFormularioEditarRol(id) {
  $.get(`/roles/editar/${id}`, (html) => {
    $("#modalRol").html(html);
    $("#modalFormRol").modal("show");
    bindFormularioRol();
  });
}

function bindFormularioRol() {
  $("#formRol").off("submit").on("submit", async function (e) {
    e.preventDefault();

    const nombreInput = $("#tipoRol");
    const nombre = nombreInput.val().trim().toLowerCase();
    const idRol = $("#formRol input[name='idRol']").val(); // puede ser vacío o null

    const esValido = await validarNombreRolUnico(nombre, idRol);

    if (!esValido) return;

    $.post("/roles/guardar", $(this).serialize(), (respuesta) => {
      if (respuesta.status === "success") {
        $("#modalFormRol").modal("hide");
        cargarRoles();
      } else if (respuesta.status === "error") {
        alert(respuesta.message); // o muestra el mensaje en el modal si deseas
      }
    });
  });
}

async function validarNombreRolUnico(nombre, idActual) {
  try {
    const roles = await $.get("/roles/all");

    const existe = roles.some(rol =>
      rol.tipoRol.trim().toLowerCase() === nombre &&
      rol.idRol != idActual
    );

    if (existe) {
      $("#tipoRol").addClass("is-invalid");

      Swal.fire({
        icon: 'warning',
        title: 'Nombre duplicado',
        text: 'Ya existe un rol con ese nombre. Usa uno diferente.',
        confirmButtonColor: '#3085d6'
      });

      return false;
    } else {
      $("#tipoRol").removeClass("is-invalid");
      return true;
    }
  } catch (error) {
    console.error("Error al validar nombre de rol:", error);
    return true; // Evita bloqueo si hay un error inesperado
  }
}



function eliminarRol(id) {
  Swal.fire({
    title: '¿Estás seguro?',
    text: 'Esta acción no se puede deshacer.',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#183D00',
    cancelButtonColor: '#d33',
    confirmButtonText: 'Sí, eliminar',
    cancelButtonText: 'Cancelar'
  }).then((result) => {
    if (result.isConfirmed) {
      $.get(`/roles/eliminar/${id}`, (response) => {
        if (response.status === 'success') {
          Swal.fire({
            icon: 'success',
            title: 'Eliminado',
            text: 'El rol ha sido eliminado correctamente.',
            timer: 2000,
            showConfirmButton: false
          });
          cargarRoles();
        } else {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo eliminar el rol.',
          });
        }
      }).fail((xhr) => {
        Swal.fire({
          icon: 'error',
          title: 'Error de servidor',
          text: xhr.responseText || 'Ocurrió un error inesperado.',
        });
      });
    }
  });
}

