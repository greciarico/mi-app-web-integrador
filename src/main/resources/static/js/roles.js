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
  $("#formRol").off("submit").on("submit", function (e) {
    e.preventDefault();
    $.post("/roles/guardar", $(this).serialize(), () => {
      $("#modalFormRol").modal("hide");
      cargarRoles();
    });
  });
}

function eliminarRol(id) {
  if (!confirm("¿Estás seguro de eliminar este rol?")) return;
  $.get(`/roles/eliminar/${id}`, () => {
    cargarRoles();
  });
}
