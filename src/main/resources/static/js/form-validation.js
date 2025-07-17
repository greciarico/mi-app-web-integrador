// ===========================================
// FUNCIONES DE VALIDACIÓN ESPECÍFICAS (PARA PRODUCTOS - sin 'codigo')
// ===========================================

async function validarFormularioProducto() {
  clearErrors('#formProducto');
  let isValid = true;
  if (!validateNombreProducto('nombre',   'nombreError',     'El nombre'))          isValid = false;
  if (!validateDescripcionProducto('descripcion','descripcionError','La descripción')) isValid = false;
  if (!validatePrecio('precio1', 'precio1Error', 'El Precio 1'))      isValid = false;
  if (!validatePrecio('precio2', 'precio2Error', 'El Precio 2'))      isValid = false;
  if (!validateStockProducto('stock', 'stockError'))                  isValid = false;
  if (!validateCategoriaProducto('categoria','categoriaError'))       isValid = false;
  return isValid;
}

function validateNombreProducto(inputElementId, errorElementId, fieldName) {
  const $input = $('#' + inputElementId);
  // 1) Si no existe el campo, no hacemos nada (devolvemos true para no bloquear otros formularios)
  if (!$input.length) return true;

  // 2) Aseguramos que val sea siempre string
  const raw = $input.val();
  const value = (typeof raw === 'string' ? raw : '').trim();

  if (value === "") {
    displayError(errorElementId, `${fieldName} es obligatorio.`);
    $input.addClass('is-invalid');
    return false;
  }

  $input.removeClass('is-invalid');
  return true;
}

function validateDescripcionProducto(inputElementId, errorElementId, fieldName) {
  const $input = $('#' + inputElementId);
  if (!$input.length) return true;
  const raw = $input.val();
  const value = (typeof raw === 'string' ? raw : '').trim();
  if (value === "") {
    displayError(errorElementId, `${fieldName} es obligatorio.`);
    $input.addClass('is-invalid');
    return false;
  }
  $input.removeClass('is-invalid');
  return true;
}

function validatePrecio(inputElementId, errorElementId, fieldName) {
  const $input = $('#' + inputElementId);
  if (!$input.length) return true;
  const raw = $input.val();
  const value = (typeof raw === 'string' ? raw : '').trim();
  const regex = /^\d+(\.\d{1,2})?$/;

  if (value === "") {
    displayError(errorElementId, `${fieldName} es obligatorio.`);
    $input.addClass('is-invalid');
    return false;
  } else if (!regex.test(value) || parseFloat(value) <= 0) {
    displayError(errorElementId, `${fieldName} debe ser un número positivo con hasta 2 decimales.`);
    $input.addClass('is-invalid');
    return false;
  }
  $input.removeClass('is-invalid');
  return true;
}

function validateStockProducto(inputElementId, errorElementId) {
  const $input = $('#' + inputElementId);
  if (!$input.length) return true;
  const raw = $input.val();
  const value = (typeof raw === 'string' ? raw : '').trim();
  const regex = /^\d+$/;

  if (value === "") {
    displayError(errorElementId, "El stock es obligatorio.");
    $input.addClass('is-invalid');
    return false;
  } else if (!regex.test(value) || parseInt(value) < 0) {
    displayError(errorElementId, "El stock debe ser un número entero no negativo.");
    $input.addClass('is-invalid');
    return false;
  }
  $input.removeClass('is-invalid');
  return true;
}

function validateCategoriaProducto(inputElementId, errorElementId) {
  const $input = $('#' + inputElementId);
  if (!$input.length) return true;
  const val = $input.val();
  if (val === null || val === "" || val === "0") {
    displayError(errorElementId, "La categoría es obligatoria.");
    $input.addClass('is-invalid');
    return false;
  }
  $input.removeClass('is-invalid');
  return true;
}

/**
 * Envía por AJAX el formulario de categoría y actualiza la lista.
 * @param {jQuery} $form — elemento jQuery del form
 */
function guardarCategoriaAjax($form) {
  const url     = $form.attr('action');
  const datos   = $form.serialize();

  $.ajax({
    type: "POST",
    url: url,
    data: datos,
    success(response) {
      if (response.status === "success") {
        mostrarNotificacion("success", response.message);
        cerrarFormularioCategoria();
        // Asume que tienes una función que recarga la tabla de categorías
        recargarListaCategorias();
      } else {
        mostrarNotificacion("error", "Error: " + response.message);
      }
    },
    error(xhr) {
      let msg = "Error al procesar la solicitud.";
      if (xhr.responseJSON?.message) {
        msg += ": " + xhr.responseJSON.message;
      }
      mostrarNotificacion("error", msg);
      console.error("Error AJAX al guardar categoría:", xhr);
    }
  });
}

/** Cierra el modal de categoría y limpia su contenido */
function cerrarFormularioCategoria() {
  $('#categoriaModal').modal('hide');
  $('#modalFormContentCategoria').empty();
  clearErrors('#formCategoria');
  console.log("Modal de producto cerrado.");
}

