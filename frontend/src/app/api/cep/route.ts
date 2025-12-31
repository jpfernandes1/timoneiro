import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  // Retrieves the 'cep' parameter from the query string.
  const searchParams = request.nextUrl.searchParams;
  const cep = searchParams.get('cep');

  // checks if the ZIP code was provided and if it has 8 digits.
  if (!cep || !/^\d{8}$/.test(cep)) {
    return NextResponse.json(
      { error: 'CEP inválido. Forneça um CEP com 8 dígitos.' },
      { status: 400 }
    );
  }

  try {
    const response = await fetch(`https://brasilapi.com.br/api/cep/v2/${cep}`, {
      // Timeout
      signal: AbortSignal.timeout(5000),
    });

    // If the response is not OK (e.g., 404 for ZIP code not found), an error is thrown.
    if (!response.ok) {
      // If 404, returns a specific error
      if (response.status === 404) {
        return NextResponse.json(
          { error: 'CEP não encontrado.' },
          { status: 404 }
        );
      }
      // To other HTTP erros, throws a generic error
      throw new Error(`Erro na API: ${response.status}`);
    }

    const data = await response.json();
    const formattedResponse = {
      cep: data.cep,
      logradouro: data.street || '',
      bairro: data.neighborhood || '',
      localidade: data.city || '',
      uf: data.state || '',
      complemento: '', 
      _originalData: data,
    };

    return NextResponse.json(formattedResponse);
  } catch (error: any) {

    console.error('Erro ao buscar CEP:', error);

    let errorMessage = 'Não foi possível consultar o CEP.';
    if (error.name === 'TimeoutError') {
      errorMessage = 'A consulta ao CEP demorou muito. Tente novamente.';
    } else if (error.name === 'AbortError') {
      errorMessage = 'A consulta foi cancelada.';
    }

    return NextResponse.json(
      {
        error: errorMessage,
        details: error.message,
        cep: cep,
      },
      { status: 500 }
    );
  }
}

// settings for the API route in Next.js
export const dynamic = 'force-dynamic'; // Ensures that each request is dynamic.
export const revalidate = 0; // Don't cache the answer